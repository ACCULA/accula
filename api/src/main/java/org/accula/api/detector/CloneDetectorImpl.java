package org.accula.api.detector;

import com.google.common.collect.Streams;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.PsiElement;
import com.suhininalex.suffixtree.Edge;
import com.suhininalex.suffixtree.SuffixTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.FileEntity;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.psi.PsiFileFactoryProvider;
import org.accula.api.psi.PsiUtils;
import org.accula.api.psi.Token;
import org.accula.api.psi.TraverseUtils;
import org.accula.api.util.Lambda;
import org.accula.api.util.Sync;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
@Slf4j
@RequiredArgsConstructor
public final class CloneDetectorImpl implements CloneDetector {
    private final ConfigProvider configProvider;
    //FIXME: blocking code on reactor threads is bad, schedule it on another executor
    private final Sync sync = new Sync();
    private final SuffixTree<Token<CommitSnapshot>> suffixTree = new SuffixTree<>();

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final Flux<FileEntity> targetFiles, final Flux<FileEntity> sourceFiles) {
        return Flux.empty();
    }

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final CommitSnapshot commitSnapshot, final Flux<FileEntity> files) {
        return addFilesToSuffixTree(files)
                .thenMany(readClonesFromSuffixTree(commitSnapshot));
    }

    @Override
    public Publisher<Void> fill(final Flux<FileEntity> files) {
        return addFilesToSuffixTree(files);
    }

    private Mono<Void> addFilesToSuffixTree(final Flux<FileEntity> files) {
        return PsiFileFactoryProvider
                .using(psiFileFactory -> files
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
                                                        .forEach(sync.writing(suffixTree::addSequence))
                                        )
                                        .then()))
                        .then());
    }

    @SuppressWarnings("UnstableApiUsage")
    private Flux<Tuple2<CodeSnippet, CodeSnippet>> readClonesFromSuffixTree(final CommitSnapshot commitSnapshot) {
        return Flux.defer(() -> {
            final var clones = sync.reading(() -> TraverseUtils
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
                            .orElse(1))
                    .map(CloneClass::new)
                    .filter(cc -> !cc.getClones().isEmpty())
                    .collect(toList()));

            return Flux
                    .fromIterable(clones.get())
                    .filter(it -> {
                        final var clone = it.getClones().get(0);
                        final var filename = clone.getTo().getFilename();
                        final var components = filename.split("/");
                        final boolean common = components[components.length - 2].equals("polis");
                        return !common && clone.getLineCount() > 3;
                    })
                    .filter(cloneClass -> cloneClass
                            .getClones()
                            .stream()
                            .anyMatch(clone -> clone.getTo().getRef().equals(commitSnapshot)))
                    .filter(cloneClass -> cloneClass
                            .getClones()
                            .stream()
                            .anyMatch(clone -> !clone.getTo().getRef().equals(commitSnapshot)))
                    .map(cloneClass -> {
                        //FIXME: issues with line numbers (many clones in same file, incorrect mapping)
                        //TODO: more efficient + take commit date into account
                        final var clns = cloneClass.getClones();
                        final var from = clns.stream().filter(clone -> !clone.getFrom().getRef().equals(commitSnapshot)).findFirst().get();
                        final var to = clns.stream().filter(clone -> clone.getFrom().getRef().equals(commitSnapshot)).findFirst().get();
                        return Tuples.of(
                                new CodeSnippet(commitSnapshot, to.getTo().getFilename(), to.getFromLine(), to.getToLine()),
                                new CodeSnippet(from.getFrom().getRef(), from.getFrom().getFilename(), from.getFromLine(), from.getToLine()));
                    });
        });
    }
}
