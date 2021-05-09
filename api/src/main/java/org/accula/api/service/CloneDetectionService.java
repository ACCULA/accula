package org.accula.api.service;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.clone.CloneDetector;
import org.accula.api.clone.CloneDetectorImpl;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileFilter;
import org.accula.api.converter.CodeToModelConverter;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.SnapshotRepo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Anton Lamtev
 */
@Slf4j
@Service
public final class CloneDetectionService {
    private final Map<Long, CloneDetector.Config> cloneDetectorConfigs = new ConcurrentHashMap<>();
    private final Map<Long, CloneDetector> cloneDetectors = new ConcurrentHashMap<>();
    private final ProjectRepo projectRepo;
    private final PullRepo pullRepo;
    private final CloneRepo cloneRepo;
    private final CodeLoader loader;
    private final SnapshotRepo snapshotRepo;

    public CloneDetectionService(final ProjectRepo projectRepo,
                                 final PullRepo pullRepo,
                                 final CloneRepo cloneRepo,
                                 final CodeLoader loader,
                                 final SnapshotRepo snapshotRepo) {
        this.projectRepo = projectRepo;
        this.projectRepo.addOnConfUpdate(this::evictConfigForProject);
        this.pullRepo = pullRepo;
        this.cloneRepo = cloneRepo;
        this.loader = loader;
        this.snapshotRepo = snapshotRepo;
    }

    public Flux<Clone> detectClones(final Long projectId, final Pull pull) {
        final var head = pull.head();

        final var clones = cloneDetector(projectId)
                .readClones(head)
                .distinct()
                .map(CodeToModelConverter::convert);

        return clones
                .collectList()
                .doOnNext(cloneList -> log.info("{} clones have been detected", cloneList.size()))
                .flatMapMany(cloneRepo::insert);
    }

    public Flux<Clone> detectClones(final Long projectId, final Pull pull, final Iterable<Snapshot> snapshots) {
        final var head = pull.head();

        final var clones = cloneDetector(projectId)
                .findClones(head, loader.loadFiles(head.repo(), snapshots, FileFilter.SRC_JAVA))
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

    public Mono<Void> fillSuffixTree(final Long projectId, final Flux<Pull> pullFlux) {
        final var files = pullFlux
                .flatMap(pull -> snapshotRepo
                        .findByPullId(pull.id())
                        .map(snapshot -> snapshot.withPull(pull)))
                .groupBy(Snapshot::repo)
                .flatMap(snapshotFlux -> snapshotFlux
                        .collectList()
                        .flatMapMany(snaps -> loader.loadFiles(snapshotFlux.key(), snaps, FileFilter.SRC_JAVA)));
        return cloneDetector(projectId).fill(files);
    }

    public void dropSuffixTree(final Long projectId) {
        cloneDetectorConfigs.remove(projectId);
        cloneDetectors.remove(projectId);
        log.info("Dropped suffix tree for project with id={}", projectId);
    }

    private Mono<Void> fillSuffixTree(final Long projectId) {
        return fillSuffixTree(projectId, pullRepo.findByProjectIdIncludingSecondaryRepos(projectId));
    }

    private CloneDetector cloneDetector(final Long projectId) {
        return cloneDetectors.computeIfAbsent(projectId, id -> new CloneDetectorImpl(cloneDetectorConfigProvider(id)));
    }

    private CloneDetector.ConfigProvider cloneDetectorConfigProvider(final Long projectId) {
        return () -> Mono
                .justOrEmpty(cloneDetectorConfigs.get(projectId))
                .switchIfEmpty(projectRepo
                        .confById(projectId)
                        .map(conf -> CloneDetector.Config.builder()
                                .cloneMinTokenCount(conf.cloneMinTokenCount())
                                .filter(FileFilter.SRC_JAVA.and(FileFilter.exclude(conf.excludedFiles())))
                                .build()))
                .doOnNext(conf -> cloneDetectorConfigs.put(projectId, conf));
    }

    private void evictConfigForProject(final Long projectId) {
        cloneDetectorConfigs.remove(projectId);
    }
}
