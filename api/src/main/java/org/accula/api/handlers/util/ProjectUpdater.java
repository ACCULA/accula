package org.accula.api.handlers.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.CommitSnapshotRepo;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.github.model.GithubApiPull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

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
    private final Scheduler processingScheduler = Schedulers.boundedElastic();
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
                    final var commitSnapshots = new HashSet<CommitSnapshot>();
                    final var pulls = new HashSet<Pull>();
                    int openPullCount = 0;

                    for (final var githubApiPull : githubApiPulls) {
                        if (!githubApiPull.isValid()) {
                            continue;
                        }

                        final var pull = processGithubApiPull(projectId, githubApiPull, users, repos, commitSnapshots);
                        pulls.add(pull);

                        if (pull.isOpen()) {
                            ++openPullCount;
                        }
                    }

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .thenMany(commitSnapshotRepo.insert(commitSnapshots))
                            .thenMany(pullRepo.upsert(pulls))
                            .thenMany(commitSnapshotRepo.mapToPulls(commitSnapshots))
                            .then(Mono.just(openPullCount));
                })
                .doOnSuccess(success -> log.info("Project has been updated successfully with pulls={}", Arrays.toString(githubApiPulls)))
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
                    final var commitSnapshots = new HashSet<CommitSnapshot>();

                    final var pull = processGithubApiPull(projectId, githubApiPull, users, repos, commitSnapshots);

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .thenMany(commitSnapshotRepo.insert(commitSnapshots))
                            .then(pullRepo.upsert(pull))
                            .thenMany(commitSnapshotRepo.mapToPulls(commitSnapshots))
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
                                      final Set<CommitSnapshot> commitSnapshots) {
        final var pull = converter.convert(githubApiPull, projectId);

        final var head = pull.getHead();
        users.add(head.getRepo().getOwner());
        repos.add(head.getRepo());
        commitSnapshots.add(head);

        final var base = pull.getBase();
        users.add(base.getRepo().getOwner());
        repos.add(base.getRepo());
        commitSnapshots.add(base);

        users.add(pull.getAuthor());

        return pull;
    }
}
