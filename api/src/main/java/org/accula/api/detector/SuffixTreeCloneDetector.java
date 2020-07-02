package org.accula.api.detector;

import com.suhininalex.suffixtree.SuffixTree;
import org.accula.api.code.FileEntity;
import org.accula.api.detector.parser.Parser;
import org.accula.api.detector.parser.Token;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.util.List;

public class SuffixTreeCloneDetector implements CloneDetector {
    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final Flux<FileEntity> targetFiles,
                                                             final Flux<FileEntity> sourceFiles) {
        return targetFiles.collectList()
                .zipWith(sourceFiles.collectList(), this::clones)
                .flatMapMany(Flux::fromIterable);
    }

    private List<Tuple2<CodeSnippet, CodeSnippet>> clones(final List<FileEntity> trg, final List<FileEntity> src) {
        final var suffixTree = new SuffixTree<Token>();
        trg
                .stream()
                .map(Parser::getFunctionsAsTokens)
                .forEach(file -> file.forEach(suffixTree::addSequence));
        src
                .stream()
                .map(Parser::getFunctionsAsTokens)
                .forEach(file -> file.forEach(suffixTree::addSequence));

        // TODO: add clone mapper
        return List.of();
    }
}
