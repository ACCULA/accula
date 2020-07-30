package org.accula.analyzer.current;

import org.accula.parser.FileEntity;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

/**
 * @author Vadim Dyachkov
 */
@FunctionalInterface
public interface CloneDetector {
    /**
     * Find clones inside {@code targetFiles} that could be copied from {@code sourceFiles}.
     * Each element emitted by the resulting {@code Flux} is a pair of:
     * - a snippet from target file
     * - a snippet from source file
     */
    Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(Flux<FileEntity> targetFiles, Flux<FileEntity> sourceFiles);
}
