package org.accula.api.service;

import lombok.RequiredArgsConstructor;
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
import reactor.util.function.Tuple2;

/**
 * @author Anton Lamtev
 */
@Service
@RequiredArgsConstructor
public final class CloneDetectionService {
    private final Scheduler processingScheduler = ReactorSchedulers.newBoundedElastic(getClass().getSimpleName());
    private final PullRepo pullRepo;
    private final CloneRepo cloneRepo;
    private final CloneDetector detector;
    private final CodeLoader loader;

    public Flux<Clone> detectClones(final Pull pull) {
        final var targetFiles = loader.loadFiles(pull.getHead(), FileFilter.JAVA);

        final var sourceFiles = pullRepo
                .findUpdatedEarlierThan(pull.getProjectId(), pull.getNumber())
                .map(Pull::getHead)
                .flatMap(head -> loader.loadFiles(head, FileFilter.JAVA));

        final var clones = detector
                .findClones(targetFiles, sourceFiles)
                .subscribeOn(processingScheduler)
                .map(this::convert);

        return clones
                .collectList()
                .flatMapMany(cloneRepo::insert);
    }

    private Clone convert(final Tuple2<CodeSnippet, CodeSnippet> clone) {
        final CodeSnippet target = clone.getT1();
        final CodeSnippet source = clone.getT2();
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
