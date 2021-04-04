package org.accula.api.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.git.GitRefs;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.PullSnapshots;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.SnapshotRepo;
import org.accula.api.github.model.GithubApiPull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

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
    private final CodeLoader codeLoader;

    public Mono<Set<Pull>> init(final List<GithubApiPull> githubApiPulls) {
        if (githubApiPulls.isEmpty()) {
            return Mono.empty();
        }

        return Mono
                .defer(() -> {
                    final var users = new HashSet<GithubUser>();
                    final var repos = new HashSet<GithubRepo>();
                    final var pulls = githubApiPulls
                            .stream()
                            .filter(pull -> pull.isValid() && pull.isNotMerged())
                            .map(pull -> processGithubApiPull(pull, users, repos))
                            .collect(Collectors.toSet());

                    return upsertUsersAndReposToDb(users, repos)
                            .then(insertPullsInfoToDb(pulls));
                })
                .doOnSuccess(pulls -> log.info("Project has been updated successfully with {} pulls", pulls.size()))
                .doOnError(e -> log.error("Failed to update project with pulls={}", githubApiPulls, e));
    }

    public Mono<PullSnapshots> update(final GithubApiPull githubApiPull) {
        return Mono
                .defer(() -> {
                    final var users = new HashSet<GithubUser>();
                    final var repos = new HashSet<GithubRepo>();
                    final var pull = processGithubApiPull(githubApiPull, users, repos);

                    return upsertUsersAndReposToDb(users, repos)
                            .then(updatePullInfoInDb(pull));
                })
                .doOnSuccess(pull -> log.info("Project has been updated successfully with {}", pull))
                .doOnError(e -> log.error("Failed to update project with pulls={}", githubApiPull, e));
    }

    private static Pull processGithubApiPull(final GithubApiPull githubApiPull,
                                             final Set<GithubUser> users,
                                             final Set<GithubRepo> repos) {
        final var pull = GithubApiToModelConverter.convert(githubApiPull);

        final var head = pull.head();
        users.add(head.repo().owner());
        repos.add(head.repo());

        final var base = pull.base();
        users.add(base.repo().owner());
        repos.add(base.repo());

        users.add(pull.author());

        return pull;
    }

    private Mono<Void> upsertUsersAndReposToDb(final Set<GithubUser> users, final Set<GithubRepo> repos) {
        return githubUserRepo.upsert(users)
                .thenMany(githubRepoRepo.upsert(repos))
                .then();
    }

    private Mono<Set<Pull>> insertPullsInfoToDb(final Set<Pull> pulls) {
        final var completeHeadSnapshots = pullHeadSnapshots(pulls);
        final var completeBaseSnapshots = completeSnapshotsWithCommits(Iterables.transform(pulls, Pull::base));
        return completeHeadSnapshots
                .zipWith(completeBaseSnapshots)
                .flatMap(headAndBaseSnapshots -> {
                    final var headSnapshots = headAndBaseSnapshots.getT1();
                    final var baseSnapshots = headAndBaseSnapshots.getT2();
                    final var snapshots = Stream.concat(
                            baseSnapshots.stream(),
                            headSnapshots
                                    .stream()
                                    .flatMap(x -> Streams.stream(x.snapshots()))
                    ).collect(Collectors.toSet());

                    pulls.removeIf(pull -> !snapshots.contains(pull.head()) ||
                                           !snapshots.contains(pull.base()));

                    return snapshotRepo.insert(snapshots)
                            .thenMany(pullRepo.upsert(pulls))
                            .thenMany(pullRepo.mapSnapshots(headSnapshots))
                            .then();
                })
                .thenReturn(pulls);
    }

    private Mono<PullSnapshots> updatePullInfoInDb(final Pull pull) {
        final var newHeadSnapshots = newHeadCommitsSnapshots(pull);
        final var baseSnapshots = baseCommitSnapshot(pull.base());
        return newHeadSnapshots
                .zipWith(baseSnapshots)
                .flatMap(commits -> {
                    final var newHeadCommitSnapshots = commits.getT1();
                    final var baseCommitSnapshot = commits.getT2();
                    final var snapshots = Iterables.concat(newHeadCommitSnapshots, List.of(baseCommitSnapshot));
                    final var pullSnapshots = PullSnapshots.of(pull, newHeadCommitSnapshots);
                    return snapshotRepo.insert(snapshots)
                            .then(pullRepo.upsert(pull))
                            .thenMany(pullRepo.mapSnapshots(pullSnapshots))
                            .then(Mono.just(pullSnapshots));
                });
    }

    private Mono<Set<Snapshot>> completeSnapshotsWithCommits(final Iterable<Snapshot> snapshots) {
        return Flux
                .fromIterable(snapshots)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(this::completeSnapshotWithCommits)
                .sequential()
                .collect(Collectors.toSet());
    }

    private Mono<List<PullSnapshots>> pullHeadSnapshots(final Iterable<Pull> pulls) {
        return Flux
                .fromIterable(pulls)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(pull -> completeSnapshotWithCommits(pull.head())
                        .collectList()
                        .filter(not(List::isEmpty))
                        .map(snapshots -> PullSnapshots.of(pull, snapshots)))
                .sequential()
                .collectList();
    }

    private Flux<Snapshot> completeSnapshotWithCommits(final Snapshot snapshot) {
        return codeLoader
                .loadCommits(snapshot.repo(), snapshot.sha())
                .map(snapshot::withCommit);
    }

    private Mono<List<Snapshot>> newHeadCommitsSnapshots(final Pull pull) {
        final var head = pull.head();
        return pullRepo.findById(pull.id())
                .flatMap(pullBeforeUpdate -> codeLoader
                        .loadCommits(head.repo(), pullBeforeUpdate.head().sha(), head.sha())
                        .map(commit -> pullBeforeUpdate.head().withCommit(commit))
                        .collectList())
                .switchIfEmpty(completeSnapshotWithCommits(head).collectList())
                .filter(not(List::isEmpty));
    }

    private Mono<Snapshot> baseCommitSnapshot(final Snapshot base) {
        return codeLoader.loadCommits(base.repo(), GitRefs.inclusive(base.sha()), base.sha())
                .map(base::withCommit)
                .next();
    }
}
