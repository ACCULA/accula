package org.accula.api.clone;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.clone.suffixtree.CloneClass;
import org.accula.api.clone.suffixtree.SuffixTreeCloneDetector;
import org.accula.api.code.FileEntity;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Snapshot;
import org.accula.api.token.Token;
import org.accula.api.token.TokenProvider;
import org.accula.api.util.Lambda;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Anton Lamtev
 */
@Slf4j
@RequiredArgsConstructor
public final class CloneDetectorImpl implements CloneDetector {
    //FIXME: avoid blocking
    private final SuffixTreeCloneDetector<Token<Snapshot>, Snapshot> suffixTreeCloneDetector = new SuffixTreeCloneDetector<>();
    private final TokenProvider<Snapshot> tokenProvider = TokenProvider.of(TokenProvider.Language.JAVA);
    private final ConfigProvider configProvider;

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final Snapshot snapshot, final Flux<FileEntity<Snapshot>> files) {
        return addFilesToSuffixTree(files)
                .thenMany(configProvider.get().flatMapMany(Lambda.passingFirstArg(this::readClonesFromSuffixTree, snapshot)));
    }

    @Override
    public Mono<Void> fill(final Flux<FileEntity<Snapshot>> files) {
        return addFilesToSuffixTree(files);
    }

    private Mono<Void> addFilesToSuffixTree(final Flux<FileEntity<Snapshot>> files) {
        return tokenProvider.tokensByMethods(files)
                .flatMap(method ->
                        Mono.fromSupplier(() -> suffixTreeCloneDetector.addTokens(method)))
                .then();
    }

    private Flux<Tuple2<CodeSnippet, CodeSnippet>> readClonesFromSuffixTree(final Snapshot snapshot, final Config config) {
        final Supplier<List<CloneClass<Snapshot>>> cloneClassesSupplier = () ->
                suffixTreeCloneDetector.cloneClassesAfterTransform(cloneClasses ->
                        cloneClasses.filter(cloneClass ->
                                cloneClassMatchesRules(cloneClass, config)
                                && cloneClassContainsClonesFromCommit(cloneClass, snapshot)
                                && cloneClassContainsClonesFromReposOtherThan(cloneClass, snapshot.getRepo())));

        return Mono
                .fromSupplier(cloneClassesSupplier)
                .flatMapMany(Flux::fromIterable)
                .map(cloneClass -> {
                    //TODO: take commit date into account
                    final var clones = cloneClass.getClones();
                    @SuppressWarnings("OptionalGetWithoutIsPresent")//
                    final var from = clones
                            .stream()
                            .filter(clone -> !clone.ref().getRepo().equals(snapshot.getRepo()))
                            .findFirst()
                            .get();
                    @SuppressWarnings("OptionalGetWithoutIsPresent")//
                    final var to = clones
                            .stream()
                            .filter(clone -> clone.ref().equals(snapshot))
                            .findFirst()
                            .get();
                    return Tuples.of(
                            new CodeSnippet(to.ref(), to.filename(), to.getFromLine(), to.getToLine()),
                            new CodeSnippet(from.ref(), from.filename(), from.getFromLine(), from.getToLine()));
                });
    }

    private static boolean cloneClassMatchesRules(final CloneClass<Snapshot> cloneClass, final Config rules) {
        return cloneClass.getLength() >= rules.getCloneMinTokenCount()
               && cloneClass
                       .getClones()
                       .stream()
                       .anyMatch(clone -> rules.getFilter().test(clone.getStart().getFilename()));
    }

    private static boolean cloneClassContainsClonesFromCommit(final CloneClass<Snapshot> cloneClass, final Snapshot commit) {
        return cloneClass
                .getClones()
                .stream()
                .anyMatch(clone -> clone.ref().equals(commit));
    }

    private static boolean cloneClassContainsClonesFromReposOtherThan(final CloneClass<Snapshot> cloneClass, final GithubRepo repo) {
        return cloneClass
                .getClones()
                .stream()
                .anyMatch(clone -> !clone.ref().getRepo().equals(repo));
    }
}
