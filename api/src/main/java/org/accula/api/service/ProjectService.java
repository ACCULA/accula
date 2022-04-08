package org.accula.api.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.CodeLoader;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
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
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
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

    //TODO: rename
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
                            .filter(GithubApiPull::isValid)
                            .map(pull -> processGithubApiPull(pull, users, repos))
                            .collect(Collectors.toSet());

                    return upsertUsersAndReposToDb(users, repos)
                            .then(insertPullsInfoToDb(pulls));
                })
                .doOnSuccess(pulls -> log.info("Project has been updated successfully with {} pulls", pulls.size()))
                .doOnError(e -> log.error("Failed to update project with pulls={}", githubApiPulls, e));
    }

    public Mono<PullSnapshots> init(final GithubApiPull githubApiPull) {
        return updateWithPull(githubApiPull, this::insertPullInfoToDb);
    }

    public Mono<PullSnapshots> updateWithNewCommits(final GithubApiPull githubApiPull) {
        return updateWithPull(githubApiPull, this::updatePullInfoInDb);
    }

    public Mono<Pull> updatePullInfo(final GithubApiPull githubApiPull) {
        return updateWithPull(githubApiPull, pullRepo::upsert);
    }

    public Mono<List<String>> headFiles(final GithubRepo repo) {
        return Mono
            .just(Project.Conf.KEEP_EXCLUDED_FILES_SYNCED)
            .concatWith(codeLoader
                .loadFilenames(repo)
                .sort())
            .collectList();
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

        users.addAll(pull.assignees());

        return pull;
    }

    private static Consumer<Signal<?>> log(final GithubApiPull pull) {
        return signal -> {
            switch (signal.getType()) {
                case ON_NEXT -> log.info("Project has been updated successfully with {}", signal.get());
                case ON_ERROR -> log.error("Failed to update project with pull={}", pull, (Throwable) signal.get());
            }
        };
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

    private <R> Mono<R> updateWithPull(final GithubApiPull githubApiPull, final Function<Pull, Mono<R>> update) {
        return Mono
            .defer(() -> {
                final var users = new HashSet<GithubUser>();
                final var repos = new HashSet<GithubRepo>();
                final var pull = processGithubApiPull(githubApiPull, users, repos);

                return upsertUsersAndReposToDb(users, repos)
                    .then(update.apply(pull));
            })
            .doOnEach(log(githubApiPull));
    }

    private Mono<PullSnapshots> insertPullInfoToDb(final Pull pull) {
        final var completeHeadSnapshots = pullHeadSnapshots(Set.of(pull))
            .filter(not(List::isEmpty))
            .map(l -> l.get(0));
        final var completeBaseSnapshots = completeSnapshotsWithCommits(List.of(pull.base()));
        return completeHeadSnapshots
            .zipWith(completeBaseSnapshots)
            .flatMap(headAndBaseSnapshots -> {
                final var headSnapshots = headAndBaseSnapshots.getT1();
                final var baseSnapshots = headAndBaseSnapshots.getT2();
                final var snapshots = Stream.concat(
                    baseSnapshots.stream(),
                    Streams.stream(headSnapshots.snapshots())
                ).collect(Collectors.toSet());

                return snapshotRepo.insert(snapshots)
                    .thenMany(pullRepo.upsert(pull))
                    .thenMany(pullRepo.mapSnapshots(headSnapshots))
                    .then(Mono.just(headSnapshots));
            })
            .switchIfEmpty(Mono.error(() -> new IllegalStateException("Unable to insert pull info to DB: " + pull)));
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
        return Flux.defer(() -> codeLoader
            .loadCommits(snapshot.repo(), snapshot.sha())
            .map(snapshot::withCommit));
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
        return codeLoader.loadCommit(base.repo(), base.sha())
            .map(base::withCommit);
    }
}
