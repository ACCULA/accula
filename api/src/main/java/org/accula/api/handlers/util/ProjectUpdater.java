package org.accula.api.handlers.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.repo.CommitSnapshotRepo;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.util.ReactorSchedulers;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ProjectUpdater {
    private final Scheduler processingScheduler = ReactorSchedulers.boundedElastic(this);
    private final GithubApiToModelConverter converter;
    private final GithubUserRepo githubUserRepo;
    private final GithubRepoRepo githubRepoRepo;
    private final CommitSnapshotRepo commitSnapshotRepo;
    private final PullRepo pullRepo;

    public Mono<Integer> update(final Long projectId, final GithubApiPull[] githubApiPulls) { // NOPMD
        if (githubApiPulls.length == 0) {
            return Mono.just(0);
        }

        return Mono
                .defer(() -> {
                    final var users = new HashSet<GithubUser>();
                    final var repos = new HashSet<GithubRepo>();
                    final var heads = new HashSet<Snapshot>();
                    final var bases = new HashSet<Snapshot>();
                    final var pulls = new HashSet<Pull>();
                    int openPullCount = 0;

                    for (final var githubApiPull : githubApiPulls) {
                        if (!githubApiPull.isValid()) {
                            continue;
                        }

                        final var pull = processGithubApiPull(projectId, githubApiPull, users, repos, heads, bases);
                        pulls.add(pull);

                        if (pull.isOpen()) {
                            ++openPullCount;
                        }
                    }

                    final var allCommitSnapshots = combine(heads, bases);

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .thenMany(commitSnapshotRepo.insert(allCommitSnapshots))
                            .thenMany(pullRepo.upsert(pulls))
                            .thenMany(commitSnapshotRepo.mapToPulls(heads))
                            .then(Mono.just(openPullCount));
                })
                .doOnSuccess(success -> log.info("Project has been updated successfully with {} pulls", githubApiPulls.length))
                .doOnError(e -> log.error("Failed to update project with pulls={}", Arrays.toString(githubApiPulls), e))
                .subscribeOn(processingScheduler);
    }

    public Mono<Pull> update(final Long projectId, final GithubApiPull githubApiPull) {
        return Mono
                .defer(() -> {
                    if (!githubApiPull.isValid()) {
                        return Mono.empty();
                    }

                    final var users = new HashSet<GithubUser>();
                    final var repos = new HashSet<GithubRepo>();
                    final var heads = new HashSet<Snapshot>();
                    final var bases = new HashSet<Snapshot>();

                    final var pull = processGithubApiPull(projectId, githubApiPull, users, repos, heads, bases);
                    final var allCommitSnapshots = combine(heads, bases);

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .thenMany(commitSnapshotRepo.insert(allCommitSnapshots))
                            .then(pullRepo.upsert(pull))
                            .thenMany(commitSnapshotRepo.mapToPulls(heads))
                            .then(Mono.just(pull));
                })
                .doOnSuccess(success -> log.info("Project has been updated successfully with pull={}", githubApiPull))
                .doOnError(e -> log.error("Failed to update project with pull={}", githubApiPull, e))
                .subscribeOn(processingScheduler);
    }

    private Pull processGithubApiPull(final Long projectId,
                                      final GithubApiPull githubApiPull,
                                      final Set<GithubUser> users,
                                      final Set<GithubRepo> repos,
                                      final Set<Snapshot> heads,
                                      final Set<Snapshot> bases) {
        final var pull = converter.convert(githubApiPull, projectId);

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
