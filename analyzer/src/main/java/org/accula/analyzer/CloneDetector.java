package org.accula.analyzer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.analyzer.checkers.SuffixTreeCloneChecker;
import org.accula.analyzer.checkers.utils.Clone;
import org.accula.analyzer.checkers.utils.ClonePair;
import org.accula.analyzer.checkers.utils.DataTransformer;
import org.accula.parser.AntlrJavaParser;
import org.antlr.v4.runtime.Token;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class CloneDetector implements Analyzer<File<String>, ClonePair> {
    private final List<File<Flux<Token>>> processedFiles = new ArrayList<>();

    @Override
    public Flux<ClonePair> analyze(final Flux<File<String>> data,
                                   final float threshold,
                                   final int minLength) {
        final var parser = new AntlrJavaParser();
        return data
                .map(file -> DataTransformer.convertString(file, parser::getTokens))
                .flatMap(this::processFile)
                .filter(clone -> clone.getNormalizedMetric() > threshold)
                .doOnError(e -> log.error("Analyzer got error: {}", e.getMessage()));
    }

    private Flux<ClonePair> processFile(final File<Flux<Token>> file2) {
        final var checker = new SuffixTreeCloneChecker();
        return Flux
                .fromIterable(processedFiles)
                .filter(f -> !f.getOwner().equals(file2.getOwner()))
                .flatMap(file1 -> {
                    var clone1 = new Clone(
                            file1.getOwner(), file1.getName(),
                            file1.getPath(), 0, 0
                    );
                    var clone2 = new Clone(
                            file2.getOwner(), file2.getName(),
                            file2.getPath(), 0, 0
                    );
                    var cloneInfo = new ClonePair(0.0f, 0, clone1, clone2);
                    return checker
                            .checkClones(file1.getContent(), file2.getContent(), cloneInfo);
                })
                .doOnComplete(() -> processedFiles.add(file2));
    }
}
