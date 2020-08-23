package org.accula.api.service;

import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileFilter;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.detector.CloneDetector;
import org.accula.api.detector.CodeSnippet;
import org.accula.api.detector.SuffixTreeCloneDetector;
import org.accula.api.util.ReactorSchedulers;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.function.TupleUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Anton Lamtev
 */
@Slf4j
@Service
public final class CloneDetectionService {
    private final Scheduler processingScheduler = ReactorSchedulers.boundedElastic(this);
    private final Map<Long, CloneDetector.Config> cloneDetectorConfigs = new ConcurrentHashMap<>();
    private final Map<Long, CloneDetector> cloneDetectors = new ConcurrentHashMap<>();
    private final ProjectRepo projectRepo;
    private final PullRepo pullRepo;
    private final CloneRepo cloneRepo;
    private final CodeLoader loader;

    public CloneDetectionService(final ProjectRepo projectRepo,
                                 final PullRepo pullRepo,
                                 final CloneRepo cloneRepo,
                                 final CodeLoader loader) {
        this.projectRepo = projectRepo;
        this.projectRepo.addOnConfUpdate(this::evictConfigForProject);
        this.pullRepo = pullRepo;
        this.cloneRepo = cloneRepo;
        this.loader = loader;
    }

    public Flux<Clone> detectClones(final Pull pull) {
        final var targetFiles = loader.loadFiles(pull.getHead(), FileFilter.SRC_JAVA);

        final var sourceFiles = pullRepo
                .findUpdatedEarlierThan(pull.getProjectId(), pull.getNumber())
                .map(Pull::getHead)
                .flatMap(head -> loader.loadFiles(head, FileFilter.SRC_JAVA));

        final var clones = cloneDetector(pull.getProjectId())
                .findClones(targetFiles, sourceFiles)
                .subscribeOn(processingScheduler)
                .map(TupleUtils.function(this::convert));

        return clones
                .collectList()
                .doOnNext(cloneList -> log.info("{} clones have been detected", cloneList.size()))
                .flatMapMany(cloneRepo::insert);
    }

    private Clone convert(final CodeSnippet target, final CodeSnippet source) {
        return Clone.builder()
                .targetSnapshot(target.getCommitSnapshot())
                .targetFile(target.getFile())
                .targetFromLine(target.getFromLine())
                .targetToLine(target.getToLine())
                .sourceSnapshot(source.getCommitSnapshot())
                .sourceFile(source.getFile())
                .sourceFromLine(source.getFromLine())
                .sourceToLine(source.getToLine())
                .build();
    }

    private CloneDetector cloneDetector(final Long projectId) {
        return cloneDetectors.computeIfAbsent(projectId, id -> new SuffixTreeCloneDetector(cloneDetectorConfigProvider(id)));
    }

    private CloneDetector.ConfigProvider cloneDetectorConfigProvider(final Long projectId) {
        return () -> Mono
                .justOrEmpty(cloneDetectorConfigs.get(projectId))
                .switchIfEmpty(projectRepo
                        .confById(projectId)
                        .map(conf -> CloneDetector.Config.builder()
                                .minCloneLength(conf.getCloneMinLineCount())
                                .build()))
                .doOnNext(conf -> cloneDetectorConfigs.put(projectId, conf));
    }

    private void evictConfigForProject(final Long projectId) {
        cloneDetectorConfigs.remove(projectId);
    }
}
