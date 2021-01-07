package org.accula.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.git.GitRefs;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.repo.CommitRepo;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.SnapshotRepo;
import org.accula.api.github.model.GithubApiPull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ProjectService {
    private final ProjectRepo projectRepo;
    private final GithubUserRepo githubUserRepo;
    private final GithubRepoRepo githubRepoRepo;
    private final SnapshotRepo snapshotRepo;
    private final PullRepo pullRepo;
    private final CommitRepo commitRepo;
    private final CodeLoader codeLoader;

    public Mono<Void> init(final Long projectId, final List<GithubApiPull> githubApiPulls) {
        if (githubApiPulls.isEmpty()) {
            return Mono.empty();
        }

        return Mono
                .defer(() -> {
                    final var users = new HashSet<GithubUser>();
                    final var repos = new HashSet<GithubRepo>();
                    final var heads = new HashSet<Snapshot>();
                    final var bases = new HashSet<Snapshot>();
                    final var pulls = githubApiPulls
                            .stream()
                            .filter(pull -> pull.isValid() && pull.isNotMerged())
                            .map(pull -> processGithubApiPull(projectId, pull, users, repos, heads, bases))
                            .collect(Collectors.toSet());

                    final var commitsMono = projectRepo
                            .findById(projectId)
                            .flatMapMany(project -> codeLoader.loadAllCommits(project.getGithubRepo()))
                            .concatWith(Flux.fromIterable(repos)
                                    .flatMap(codeLoader::loadAllCommits)
                                    .parallel()
                                    .runOn(Schedulers.parallel()))
                            .collect(Collectors.toSet());

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .then(commitsMono
                                    .flatMap(commits -> {
                                        pulls.removeIf(pull -> !commits.contains(pull.getHead().getCommit()) ||
                                                               !commits.contains(pull.getBase().getCommit()));
                                        bases.removeIf(base -> !commits.contains(base.getCommit()));
                                        heads.removeIf(head -> !commits.contains(head.getCommit()) ||
                                                               pulls.stream().noneMatch(pull -> pull.getId().equals(head.getPullId())));

                                        return commitRepo
                                                .insert(commits)
                                                .thenMany(snapshotRepo.insert(combine(heads, bases)))
                                                .thenMany(pullRepo.upsert(pulls))
                                                .thenMany(snapshotRepo.mapToPulls(heads))
                                                .doOnComplete(() ->
                                                        log.info("Project has been updated successfully with {} pulls", pulls.size()))
                                                .doOnError(e -> log.error("Failed to update project with pulls={}", githubApiPulls, e))
                                                .then();
                                    }));
                });
    }

    public Mono<Pull> update(final Long projectId, final GithubApiPull githubApiPull) {
        return Mono
                .defer(() -> {
                    final var users = new HashSet<GithubUser>();
                    final var repos = new HashSet<GithubRepo>();
                    final var heads = new HashSet<Snapshot>();
                    final var bases = new HashSet<Snapshot>();

                    final var pull = processGithubApiPull(projectId, githubApiPull, users, repos, heads, bases);
                    final var head = pull.getHead();
                    final var base = pull.getBase();

                    final var commitsMono = pullRepo.findById(pull.getId())
                            .flatMapMany(pullBeforeUpdate -> codeLoader
                                    .loadCommits(head.getRepo(), pullBeforeUpdate.getHead().getSha(), head.getSha()))
                            .concatWith(codeLoader.loadCommits(base.getRepo(), GitRefs.inclusive(base.getSha()), base.getSha()))
                            .collect(Collectors.toSet());

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .then(commitsMono.flatMap(commits -> {
                                final var completePull = pullFilledWithCommits(pull, commits);
                                return commitRepo.insert(commits)
                                        .thenMany(snapshotRepo.insert(combine(bases, heads)))
                                        .then(pullRepo.upsert(completePull))
                                        .thenMany(snapshotRepo.mapToPulls(heads))
                                        .then(Mono.just(completePull));
                            }));
                })
                .doOnSuccess(pull -> log.info("Project has been updated successfully with {}", pull))
                .doOnError(e -> log.error("Failed to update project with pulls={}", githubApiPull, e));
    }

    private static Pull processGithubApiPull(final Long projectId,
                                             final GithubApiPull githubApiPull,
                                             final Set<GithubUser> users,
                                             final Set<GithubRepo> repos,
                                             final Set<Snapshot> heads,
                                             final Set<Snapshot> bases) {
        final var pull = GithubApiToModelConverter.convert(githubApiPull, projectId);

        final var head = pull.getHead();
        users.add(head.getRepo().getOwner());
        repos.add(head.getRepo());
        heads.add(head);

        final var base = pull.getBase();
        users.add(base.getRepo().getOwner());
        repos.add(base.getRepo());
        bases.add(base);

        users.add(pull.getAuthor());

        return pull;
    }

    private static <T> Set<T> combine(final Set<T> first, final Set<T> second) {
        final var combined = new HashSet<T>(first.size() + second.size());
        combined.addAll(first);
        combined.addAll(second);
        return combined;
    }

    private static Pull pullFilledWithCommits(final Pull pull, final Set<Commit> commits) {
        final var head = pull.getHead();
        final var base = pull.getBase();

        Commit headCommit = null;
        Commit baseCommit = null;

        for (final var commit : commits) {
            if (headCommit != null && baseCommit != null) {
                break;
            }
            if (head.getCommit().equals(commit)) {
                headCommit = commit;
            } else if (base.getCommit().equals(commit)) {
                baseCommit = commit;
            }
        }

        return pull
                .toBuilder()
                .head(head
                        .toBuilder()
                        .commit(Objects.requireNonNull(headCommit))
                        .build())
                .base(base
                        .toBuilder()
                        .commit(Objects.requireNonNull(baseCommit))
                        .build())
                .build();
    }
}
