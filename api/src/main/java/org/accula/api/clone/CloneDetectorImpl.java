package org.accula.api.clone;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.FileEntity;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.clone.suffixtree.CloneClass;
import org.accula.api.clone.suffixtree.SuffixTreeCloneDetector;
import org.accula.api.token.Token;
import org.accula.api.token.TokenProvider;
import org.accula.api.util.Lambda;
import org.accula.api.util.ReactorSchedulers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
@Slf4j
@RequiredArgsConstructor
public final class CloneDetectorImpl implements CloneDetector {
    private final Scheduler scheduler = ReactorSchedulers.boundedElastic(this);
    private final SuffixTreeCloneDetector<Token<CommitSnapshot>, CommitSnapshot> suffixTreeCloneDetector = new SuffixTreeCloneDetector<>();
    private final TokenProvider<CommitSnapshot> tokenProvider = TokenProvider.of(TokenProvider.Language.JAVA);
    private final ConfigProvider configProvider;

    @Override
    public Flux<Tuple2<CodeSnippet, CodeSnippet>> findClones(final CommitSnapshot commitSnapshot,
                                                             final Flux<FileEntity<CommitSnapshot>> files) {
        return addFilesToSuffixTree(files)
                .thenMany(configProvider.get().flatMapMany(Lambda.passingFirstArg(this::readClonesFromSuffixTree, commitSnapshot)));
    }

    @Override
    public Mono<Void> fill(final Flux<FileEntity<CommitSnapshot>> files) {
        return addFilesToSuffixTree(files);
    }

    private Mono<Void> addFilesToSuffixTree(final Flux<FileEntity<CommitSnapshot>> files) {
        return tokenProvider.tokensByMethods(files)
                .map(stream -> stream.collect(toList()))
                .flatMap(methods ->
                        Mono.fromRunnable(() -> suffixTreeCloneDetector.addTokens(methods))
                                .subscribeOn(scheduler))
                .then();
    }

    private Flux<Tuple2<CodeSnippet, CodeSnippet>> readClonesFromSuffixTree(final CommitSnapshot commitSnapshot, final Config config) {
        final Supplier<List<CloneClass<CommitSnapshot>>> cloneClassesSupplier = () ->
                suffixTreeCloneDetector.cloneClassesAfterTransform(cloneClasses ->
                        cloneClasses.filter(cloneClass ->
                                cloneClassMatchesRules(cloneClass, config)
                                && cloneClassContainsClonesFromCommit(cloneClass, commitSnapshot)
                                && cloneClassContainsClonesFromReposOtherThan(cloneClass, commitSnapshot.getRepo())));

        return Mono
                .fromSupplier(cloneClassesSupplier)
                .subscribeOn(scheduler)
                .flatMapMany(Flux::fromIterable)
                .map(cloneClass -> {
                    //TODO: take commit date into account
                    final var clones = cloneClass.getClones();
                    @SuppressWarnings("OptionalGetWithoutIsPresent")//
                    final var from = clones
                            .stream()
                            .filter(clone -> !clone.ref().getRepo().equals(commitSnapshot.getRepo()))
                            .findFirst()
                            .get();
                    @SuppressWarnings("OptionalGetWithoutIsPresent")//
                    final var to = clones
                            .stream()
                            .filter(clone -> clone.ref().equals(commitSnapshot))
                            .findFirst()
                            .get();
                    return Tuples.of(
                            new CodeSnippet(to.ref(), to.filename(), to.getFromLine(), to.getToLine()),
                            new CodeSnippet(from.ref(), from.filename(), from.getFromLine(), from.getToLine()));
                });
    }

    private static boolean cloneClassMatchesRules(final CloneClass<CommitSnapshot> cloneClass, final Config rules) {
        return cloneClass.getLength() >= rules.getMinCloneLength()
               && cloneClass
                       .getClones()
                       .stream()
                       .anyMatch(clone -> rules.getFilter().test(clone.getStart().getFilename()));
    }

    private static boolean cloneClassContainsClonesFromCommit(final CloneClass<CommitSnapshot> cloneClass, final CommitSnapshot commit) {
        return cloneClass
                .getClones()
                .stream()
                .anyMatch(clone -> clone.ref().equals(commit));
    }

    private static boolean cloneClassContainsClonesFromReposOtherThan(final CloneClass<CommitSnapshot> cloneClass, final GithubRepo repo) {
        return cloneClass
                .getClones()
                .stream()
                .anyMatch(clone -> !clone.ref().getRepo().equals(repo));
    }
}
