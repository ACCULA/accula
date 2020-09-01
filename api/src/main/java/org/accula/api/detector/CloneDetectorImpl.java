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
import org.accula.api.detector.psi.PsiFileFactoryProvider;
import org.accula.api.detector.psi.PsiUtils;
import org.accula.api.detector.psi.Token;
import org.accula.api.detector.psi.TraverseUtils;
import org.accula.api.util.Lambda;
import org.accula.api.util.ReactorSchedulers;
import org.accula.api.util.Sync;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
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
    private final Scheduler scheduler = ReactorSchedulers.boundedElastic(this);
    private final Sync sync = new Sync();
    private final SuffixTree<Token<CommitSnapshot>> suffixTree = new SuffixTree<>();
    private final ConfigProvider configProvider;

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final Flux<FileEntity> targetFiles, final Flux<FileEntity> sourceFiles) {
        return Flux.empty();
    }

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final CommitSnapshot commitSnapshot, final Flux<FileEntity> files) {
        return addFilesToSuffixTree(files)
                .thenMany(configProvider.get().flatMapMany(Lambda.passingFirstArg(this::readClonesFromSuffixTree, commitSnapshot)));
    }

    @Override
    public Mono<Void> fill(final Flux<FileEntity> files) {
        return addFilesToSuffixTree(files);
    }

    private Mono<Void> addFilesToSuffixTree(final Flux<FileEntity> files) {
        return PsiFileFactoryProvider
                .using(psiFileFactory -> files
                        .flatMap(file -> Mono
                                .fromSupplier(() -> psiFileFactory.createFileFromText(file.getName(), JavaLanguage.INSTANCE, file.getContent()))
                                .subscribeOn(scheduler)
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
                                        .subscribeOn(scheduler)
                                        .then()))
                        .then());
    }

    @SuppressWarnings("UnstableApiUsage")
    private Flux<Tuple2<CodeSnippet, CodeSnippet>> readClonesFromSuffixTree(final CommitSnapshot commitSnapshot, final Config config) {
        final var cloneNodes = TraverseUtils
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

        final var cloneClasses = sync.reading(() -> cloneNodes
                .map(CloneClass::new)
                .filter(cc -> !cc.getClones().isEmpty())
                .collect(toList()));

        return Mono
                .fromSupplier(cloneClasses)
                .subscribeOn(scheduler)
                .flatMapMany(Flux::fromIterable)
                .filter(it -> {
                    final var clone = it.getClones().get(0);
                    final var filename = clone.getTo().getFilename();
                    final var components = filename.split("/");
                    final boolean common = components[components.length - 2].equals("polis");
                    return !common && clone.getLineCount() >= config.getMinCloneLength();
                })
                .filter(cloneClass -> cloneClass.getClones().stream().anyMatch(clone -> !clone.getTo().getRef().getRepo().equals(commitSnapshot.getRepo())))
                .filter(cloneClass -> cloneClass.getClones().stream().distinct().count() > 1)
                .filter(cloneClass -> cloneClass.getClones().stream().anyMatch(clone -> clone.getFrom().getRef().equals(commitSnapshot)))
                .map(cloneClass -> {
                    //FIXME: issues with line numbers (many clones in same file, incorrect mapping)
                    //TODO: more efficient + take commit date into account
                    final var clns = cloneClass.getClones();
                    final var from = clns.stream().filter(clone -> !clone.getFrom().getRef().getRepo().equals(commitSnapshot.getRepo())).findFirst().get();
                    final var to = clns.stream().filter(clone -> clone.getFrom().getRef().equals(commitSnapshot)).findFirst().get();
                    return Tuples.of(
                            new CodeSnippet(to.getTo().getRef(), to.getTo().getFilename(), to.getFromLine(), to.getToLine()),
                            new CodeSnippet(from.getFrom().getRef(), from.getFrom().getFilename(), from.getFromLine(), from.getToLine()));
                });
    }
}
