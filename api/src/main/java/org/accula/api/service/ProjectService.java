package org.accula.api.service;

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
import org.accula.api.util.Iterables;
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

                    final var commitSnapshotsMono = completeSnapshotsWithCommits(bases)
                            .zipWith(pullSnapshots(pulls));

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .then(commitSnapshotsMono
                                    .flatMap(snapshotMap -> {
                                        final var pullSnapshots = snapshotMap.getT2();
                                        final var snapshots = Stream.concat(
                                                snapshotMap.getT1().stream(),
                                                pullSnapshots
                                                        .stream()
                                                        .flatMap(x -> Streams.stream(x.snapshots()))
                                        ).collect(Collectors.toSet());

                                        pulls.removeIf(pull -> !snapshots.contains(pull.head()) ||
                                                               !snapshots.contains(pull.base()));

                                        return snapshotRepo.insert(snapshots)
                                                .thenMany(pullRepo.upsert(pulls))
                                                .thenMany(pullRepo.mapSnapshots(pullSnapshots))
                                                .doOnComplete(() ->
                                                        log.info("Project has been updated successfully with {} pulls", pulls.size()))
                                                .doOnError(e -> log.error("Failed to update project with pulls={}", githubApiPulls, e))
                                                .then();
                                    }));
                });
    }

    public Mono<PullSnapshots> update(final Long projectId, final GithubApiPull githubApiPull) {
        return Mono
                .defer(() -> {
                    final var users = new HashSet<GithubUser>();
                    final var repos = new HashSet<GithubRepo>();
                    final var heads = new HashSet<Snapshot>();
                    final var bases = new HashSet<Snapshot>();

                    final var pull = processGithubApiPull(projectId, githubApiPull, users, repos, heads, bases);
                    final var head = pull.head();
                    final var base = pull.base();

                    final var commitsMono = pullRepo.findById(pull.id())
                            .flatMap(pullBeforeUpdate -> codeLoader
                                    .loadCommits(head.repo(), pullBeforeUpdate.head().sha(), head.sha())
                                    .map(commit -> pullBeforeUpdate.head().withCommit(commit))
                                    .collectList())
                            .switchIfEmpty(completeSnapshotWithCommits(head).collectList())
                            .filter(not(List::isEmpty))
                            .zipWith(codeLoader.loadCommits(base.repo(), GitRefs.inclusive(base.sha()), base.sha())
                                    .map(base::withCommit)
                                    .next());

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .then(commitsMono.flatMap(commits -> {
                                final var newHeadCommitSnapshots = commits.getT1();
                                final var baseCommitSnapshot = commits.getT2();
                                final var snapshots = Iterables.withHead(newHeadCommitSnapshots, baseCommitSnapshot);
                                return snapshotRepo.insert(snapshots)
                                        .then(pullRepo.upsert(pull))
                                        .thenMany(pullRepo.mapSnapshots(PullSnapshots.of(pull, newHeadCommitSnapshots)))
                                        .then(Mono.just(PullSnapshots.of(pull, newHeadCommitSnapshots)));
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

        final var head = pull.head();
        users.add(head.repo().owner());
        repos.add(head.repo());
        heads.add(head);

        final var base = pull.base();
        users.add(base.repo().owner());
        repos.add(base.repo());
        bases.add(base);

        users.add(pull.author());

        return pull;
    }

    private Mono<List<PullSnapshots>> pullSnapshots(final Iterable<Pull> pulls) {
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

    private Mono<Set<Snapshot>> completeSnapshotsWithCommits(final Iterable<Snapshot> snapshots) {
        return Flux
                .fromIterable(snapshots)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(this::completeSnapshotWithCommits)
                .sequential()
                .collect(Collectors.toSet());
    }

    private Flux<Snapshot> completeSnapshotWithCommits(final Snapshot snapshot) {
        return codeLoader
                .loadCommits(snapshot.repo(), snapshot.sha())
                .map(snapshot::withCommit);
    }
}
