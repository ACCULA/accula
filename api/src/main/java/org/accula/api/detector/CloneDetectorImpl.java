package org.accula.api.detector;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.PsiElement;
import com.suhininalex.suffixtree.SuffixTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.FileEntity;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
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
                                .fromSupplier(() -> psiFileFactory
                                        .createFileFromText(file.getName(), JavaLanguage.INSTANCE, file.getContent()))
                                .subscribeOn(scheduler)
                                .flatMap(psiFile -> Mono
                                        .fromRunnable(() ->
                                                PsiUtils.methodBodies(psiFile)
                                                        .stream()
                                                        .map(method -> TraverseUtils
                                                                .dfs(method, TraverseUtils.stream(PsiElement::getChildren))
                                                                .filter(PsiUtils::isValuableToken)
                                                                .map(Lambda.passingTailArg(PsiUtils::token, file.getCommitSnapshot()))
                                                                .collect(toList()))
                                                        .forEach(sync.writing(suffixTree::addSequence))
                                        )
                                        .subscribeOn(scheduler)
                                        .then()))
                        .then());
    }

    private Flux<Tuple2<CodeSnippet, CodeSnippet>> readClonesFromSuffixTree(final CommitSnapshot commitSnapshot, final Config config) {
        final var cloneNodes = TraverseUtils
                .dfs(suffixTree.getRoot(), SuffixTreeUtils::terminalNodes)
                .filter(SuffixTreeUtils::isCloneNode);

        final var cloneClasses = sync.reading(() -> cloneNodes
                .map(CloneClass::new)
                .filter(cloneClass -> !cloneClass.getClones().isEmpty())
                .collect(toList()));

        return Mono
                .fromSupplier(cloneClasses)
                .subscribeOn(scheduler)
                .flatMapMany(Flux::fromIterable)
                .filter(cloneClass -> cloneClassMatchesRules(cloneClass, config)
                                      && cloneClassContainsClonesFromCommit(cloneClass, commitSnapshot)
                                      && cloneClassContainsClonesFromReposOtherThan(cloneClass, commitSnapshot.getRepo()))
                .map(cloneClass -> {
                    //TODO: take commit date into account
                    //TODO: May be we need to return all the clone classes with all clones here,
                    // and then filter them in next place, CloneDetectionService, e.g.
                    final var clones = cloneClass.getClones();
                    @SuppressWarnings("OptionalGetWithoutIsPresent")//
                    final var from = clones
                            .stream()
                            .filter(clone -> !clone.commitSnapshot().getRepo().equals(commitSnapshot.getRepo()))
                            .findFirst()
                            .get();
                    @SuppressWarnings("OptionalGetWithoutIsPresent")//
                    final var to = clones
                            .stream()
                            .filter(clone -> clone.commitSnapshot().equals(commitSnapshot))
                            .findFirst()
                            .get();
                    return Tuples.of(
                            new CodeSnippet(to.commitSnapshot(), to.filename(), to.getFromLine(), to.getToLine()),
                            new CodeSnippet(from.commitSnapshot(), from.filename(), from.getFromLine(), from.getToLine()));
                });
    }

    private static boolean cloneClassMatchesRules(final CloneClass cloneClass, final Config rules) {
        return cloneClass.getLength() >= rules.getMinCloneLength()
               && cloneClass
                       .getClones()
                       .stream()
                       .anyMatch(clone -> rules.getFilter().test(clone.getStart().getFilename()));
    }

    private static boolean cloneClassContainsClonesFromCommit(final CloneClass cloneClass, final CommitSnapshot commit) {
        return cloneClass
                .getClones()
                .stream()
                .anyMatch(clone -> clone.commitSnapshot().equals(commit));
    }

    private static boolean cloneClassContainsClonesFromReposOtherThan(final CloneClass cloneClass, final GithubRepo repo) {
        return cloneClass
                .getClones()
                .stream()
                .anyMatch(clone -> !clone.commitSnapshot().getRepo().equals(repo));
    }
}
