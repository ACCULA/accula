package org.accula.api.detector;

import com.google.common.collect.Streams;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.PsiElement;
import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.SuffixTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.FileEntity;
import org.accula.api.psi.Clone;
import org.accula.api.psi.CloneClass;
import org.accula.api.psi.PsiUtils;
import org.accula.api.psi.SuffixTreeUtils;
import org.accula.api.psi.Token;
import org.accula.api.psi.TraverseUtils;
import org.accula.api.util.Lambda;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
@Slf4j
@RequiredArgsConstructor
public final class CloneDetectorImpl implements CloneDetector {
    private final ConfigProvider configProvider;
    private final SuffixTree<Token> suffixTree = new SuffixTree<>();

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final Flux<FileEntity> targetFiles, final Flux<FileEntity> sourceFiles) {
        return Flux.empty();
    }

    @Override
    public Flux<Clone> findClones(final Flux<FileEntity> files) {
        return addFilesToSuffixTree(files)
                .thenMany(readClonesFromSuffixTree());
    }

    @Override
    public Publisher<Void> fill(final Flux<FileEntity> files) {
        return addFilesToSuffixTree(files);
    }

    private Mono<Void> addFilesToSuffixTree(final Flux<FileEntity> files) {
        return PsiUtils
                .withFileFactory(psiFileFactory -> files
                        .flatMap(file -> Mono
                                .fromSupplier(() -> psiFileFactory.createFileFromText(file.getName(), JavaLanguage.INSTANCE, file.getContent()))
                                .flatMap(psiFile -> Mono
                                        .fromRunnable(() ->
                                                PsiUtils.methodBodies(psiFile)
                                                        .stream()
                                                        .map(method -> TraverseUtils.dfs(method, TraverseUtils.stream(PsiElement::getChildren))
                                                                .filter(PsiUtils::isValuableToken)
                                                                .map(Lambda.passingTailArg(PsiUtils::token, file.getCommitSnapshot()))
                                                                .collect(toList()))
                                                        .forEach(suffixTree::addSequence)
                                        ))))
                .then();
    }

    @SuppressWarnings("UnstableApiUsage")
    private Flux<Clone> readClonesFromSuffixTree() {
        return Flux.defer(() -> {
            final var clones = TraverseUtils
                    .dfs(suffixTree.getRoot(), node -> node
                            .getEdges()
                            .stream()
                            .map(Edge::getTerminal)
                            .filter(Objects::nonNull))
                    .filter(node -> node
                            .getEdges()
                            .stream()
                            .allMatch(edge -> edge.getBegin() == edge.getEnd() && edge.getBegin() == edge.getSequence().size() - 1))
                    .filter(node -> 0 == Streams.findLast(SuffixTreeUtils.parentEdges(node))
                            .map(Edge::getBegin)
                            .orElse(1));

            final var filtered = clones
                    .map(CloneClass::new)
                    .filter(cc -> !cc.getClones().isEmpty());

            return Flux
                    .fromStream(filtered)
                    .filter(it -> {
                        final var clone = it.getClones().get(0);
                        final var filename = clone.getTo().getFilename();
                        final var components = filename.split("/");
                        final boolean common = components[components.length - 2].equals("polis");
                        return !common && clone.getLineCount() > 3;
                    })
                    .flatMap(it -> Flux
                            .fromStream(it.getClones()
                                    .stream()));
        });
    }
}
