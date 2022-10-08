package org.accula.api.service;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.clone.CloneDetector;
import org.accula.api.clone.CloneDetectorImpl;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileFilter;
import org.accula.api.code.Languages;
import org.accula.api.converter.CodeToModelConverter;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.SnapshotRepo;
import org.accula.api.util.Checks;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * @author Anton Lamtev
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class CloneDetectionService {
    private final Map<Long, CloneDetector.Config> cloneDetectorConfigs = new ConcurrentHashMap<>();
    private final Map<Long, CloneDetector> cloneDetectors = new ConcurrentHashMap<>();
    private final ProjectRepo projectRepo;
    private final PullRepo pullRepo;
    private final CloneRepo cloneRepo;
    private final CodeLoader loader;
    private final SnapshotRepo snapshotRepo;

    @PostConstruct
    private void init() {
        projectRepo.addOnConfUpdate(this::evictConfigForProject);
    }

    public Flux<Clone> readClonesAndSaveToDb(final Pull pull) {
        final var commitsToExclude = loader.loadAllCommitsSha(pull.base().repo()).cache();
        final var clones = cloneDetector(Checks.notNull(pull.primaryProjectId(), "Pull primaryProjectId"))
            .flatMapMany(cloneDetector -> cloneDetector.readClones(pull.head()))
            .filterWhen(codeClone -> commitsToExclude.map(commits -> !commits.contains(codeClone.source().snapshot().sha())))
            .distinct()
            .map(CodeToModelConverter::convert);

        return clones
                .collectList()
                .doOnNext(cloneList -> log.info("{} clones have been detected", cloneList.size()))
                .flatMapMany(cloneRepo::insert);
    }

    public Flux<Clone> detectClonesInNewFilesAndSaveToDb(final Long projectId, final Pull pull, final Iterable<Snapshot> snapshots) {
        final var commitsToExclude = loader.loadAllCommitsSha(pull.base().repo()).cache();
        final var head = pull.head();
        final var clones = with(projectId, (detector, config) -> detector
            .findClones(head, loader.loadFiles(head.repo(), snapshots, config.languageFilter()), snapshots))
            .filterWhen(codeClone -> commitsToExclude.map(commits -> !commits.contains(codeClone.source().snapshot().sha())))
            .distinct()
            .map(CodeToModelConverter::convert);

        return clones
            .collectList()
            .doOnNext(cloneList -> log.info("{} clones have been detected", cloneList.size()))
            .flatMapMany(cloneRepo::insert);
    }

    public Mono<Void> fillSuffixTree() {
        return projectRepo
                .getTop(100)
                .flatMap(project -> fillSuffixTree(project.id()))
                .then();
    }

    // TODO: добавлять в дерево все файлы из коммитов в репке проекта

    public Mono<Void> fillSuffixTree(final Long projectId, final Flux<Pull> pullFlux) {
        return with(projectId, (detector, config) -> Flux.from(detector.fill(
            pullFlux
                .flatMap(pull -> snapshotRepo
                    .findByPullId(pull.id())
                    .map(snapshot -> snapshot.withPull(pull))
                )
                .groupBy(Snapshot::repo)
                .flatMap(snapshotFlux -> snapshotFlux
                    .collectList()
                    .flatMapMany(snaps -> loader.loadFiles(snapshotFlux.key(), snaps, config.languageFilter()))
                )
                .concatWith(projectRepo
                    .findById(projectId)
                    .flatMapMany(project -> snapshotRepo
                        .findByRepoId(project.githubRepo().id())
                        .collectList()
                        .flatMapMany(snaps -> loader.loadFiles(project.githubRepo(), snaps, config.languageFilter()))))
                .subscribeOn(Schedulers.parallel())
        ))).then();
    }

    public void dropSuffixTree(final Long projectId) {
        cloneDetectorConfigs.remove(projectId);
        cloneDetectors.remove(projectId);
        log.info("Dropped suffix tree for project with id={}", projectId);
    }

    private Mono<Void> fillSuffixTree(final Long projectId) {
        return fillSuffixTree(projectId, pullRepo.findByProjectIdIncludingSecondaryRepos(projectId));
    }

    private Mono<CloneDetector> cloneDetector(final Long projectId) {
        return Mono
            .justOrEmpty(cloneDetectors.get(projectId))
            .switchIfEmpty(projectRepo
                .findById(projectId)
                .map(project -> new CloneDetectorImpl(
                    project.githubRepo().identity(),
                    cloneDetectorConfigProvider(projectId))
                )
            )
            .map(detector -> cloneDetectors.computeIfAbsent(projectId, __ -> detector));
    }

    private CloneDetector.ConfigProvider cloneDetectorConfigProvider(final Long projectId) {
        return () -> Mono
                .justOrEmpty(cloneDetectorConfigs.get(projectId))
                .switchIfEmpty(projectRepo
                        .confById(projectId)
                        .map(conf -> CloneDetector.Config.builder()
                                .cloneMinTokenCount(conf.cloneMinTokenCount())
                                .filter(FileFilter.notIn(conf.excludedFiles()))
                                .languages(conf.languages())
                                .languageFilter(Languages.filter(conf.languages()))
                                .excludedSourceAuthors(new LongOpenHashSet(conf.excludedSourceAuthorIds())::contains)
                                .build()))
                .map(conf -> cloneDetectorConfigs.computeIfAbsent(projectId, __ -> conf));
    }

    private <R> Flux<R> with(final Long projectId, final BiFunction<CloneDetector, CloneDetector.Config, Flux<R>> use) {
        return cloneDetector(projectId)
            .zipWith(cloneDetectorConfigProvider(projectId).get())
            .flatMapMany(TupleUtils.function(use));
    }

    private void evictConfigForProject(final Long projectId) {
        cloneDetectorConfigs.remove(projectId);
    }
}
