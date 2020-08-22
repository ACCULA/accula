package org.accula.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileFilter;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.detector.CloneDetector;
import org.accula.api.detector.CodeSnippet;
import org.accula.api.util.ReactorSchedulers;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;

/**
 * @author Anton Lamtev
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class CloneDetectionService {
    private final Scheduler processingScheduler = ReactorSchedulers.boundedElastic(this);
    private final PullRepo pullRepo;
    private final CloneRepo cloneRepo;
    private final CloneDetector detector;
    private final CodeLoader loader;

    public Flux<Clone> detectClones(final Pull pull) {
        final var targetFiles = loader.loadFiles(pull.getHead(), FileFilter.SRC_JAVA);

        final var sourceFiles = pullRepo
                .findUpdatedEarlierThan(pull.getProjectId(), pull.getNumber())
                .map(Pull::getHead)
                .flatMap(head -> loader.loadFiles(head, FileFilter.SRC_JAVA));

        final var clones = detector
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
}
