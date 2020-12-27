package org.accula.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.SnapshotRepo;
import org.accula.api.github.model.GithubApiPull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ProjectService {
    private final GithubUserRepo githubUserRepo;
    private final GithubRepoRepo githubRepoRepo;
    private final SnapshotRepo snapshotRepo;
    private final PullRepo pullRepo;

    public Flux<Pull> update(final Long projectId, final List<GithubApiPull> githubApiPulls) {
        if (githubApiPulls.isEmpty()) {
            return Flux.empty();
        }

        return Flux
                .defer(() -> {
                    final var users = new HashSet<GithubUser>();
                    final var repos = new HashSet<GithubRepo>();
                    final var heads = new HashSet<Snapshot>();
                    final var bases = new HashSet<Snapshot>();
                    final var pulls = new HashSet<Pull>();

                    for (final var githubApiPull : githubApiPulls) {
                        if (!githubApiPull.isValid()) {
                            continue;
                        }

                        final var pull = processGithubApiPull(projectId, githubApiPull, users, repos, heads, bases);
                        pulls.add(pull);
                    }

                    final var allCommitSnapshots = combine(heads, bases);

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .thenMany(snapshotRepo.insert(allCommitSnapshots))
                            .thenMany(pullRepo.upsert(pulls))
                            .thenMany(snapshotRepo.mapToPulls(heads))
                            .thenMany(Flux.fromIterable(pulls));
                })
                .doOnComplete(() -> log.info("Project has been updated successfully with {} pulls", githubApiPulls.size()))
                .doOnError(e -> log.error("Failed to update project with pulls={}", githubApiPulls, e));
    }

    public Mono<Pull> update(final Long projectId, final GithubApiPull githubApiPull) {
        return update(projectId, List.of(githubApiPull))
                .next()
                .doOnNext(pull -> log.info("Project has been updated successfully with {}", pull));
    }

    private Pull processGithubApiPull(final Long projectId,
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
}
