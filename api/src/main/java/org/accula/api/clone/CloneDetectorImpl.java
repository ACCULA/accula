package org.accula.api.clone;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.clone.suffixtree.Clone;
import org.accula.api.clone.suffixtree.CloneClass;
import org.accula.api.clone.suffixtree.SuffixTreeCloneDetector;
import org.accula.api.code.FileEntity;
import org.accula.api.db.model.Snapshot;
import org.accula.api.token.Token;
import org.accula.api.token.TokenProvider;
import org.accula.api.token.TokenProvider.Language;
import org.accula.api.util.Comparators;
import org.accula.api.util.Lambda;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
@Slf4j
@RequiredArgsConstructor
public final class CloneDetectorImpl implements CloneDetector {
    //FIXME: avoid blocking
    private final SuffixTreeCloneDetector<Token<Snapshot>, Snapshot> suffixTreeCloneDetector = new SuffixTreeCloneDetector<>();
    private final TokenProvider<Snapshot> tokenProvider = TokenProvider.of(Language.JAVA);
    private final ConfigProvider configProvider;

    @Override
    public Flux<CodeClone> readClones(final Snapshot pullSnapshot) {
        return configProvider.get()
                .flatMapMany(Lambda.passingFirstArg(this::readClonesFromSuffixTree, pullSnapshot));
    }

    @Override
    public Flux<CodeClone> findClones(final Snapshot pullSnapshot, final Flux<FileEntity<Snapshot>> files) {
        return addFilesToSuffixTree(files)
                .thenMany(readClones(pullSnapshot));
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

    //TODO:
    // Clearer api: here snapshot is something that references to concrete pull request, not commit
    private Flux<CodeClone> readClonesFromSuffixTree(final Snapshot snapshot, final Config config) {
        final Supplier<List<CodeClone>> cloneClassesSupplier = () ->
                suffixTreeCloneDetector.transform(cloneClasses -> cloneClasses
                        .filter(cloneClass -> cloneClassMatchesRules(cloneClass, config) &&
                                              cloneClassContainsClonesForSnapshot(cloneClass, snapshot))
                        .flatMap(cloneClass -> {
                            final var clones = cloneClass.clones();
                            final var source = clones
                                    .stream()
                                    .reduce(Comparators.minBy(
                                            clone -> clone.ref().commit().date(),
                                            clone -> clone.ref().pullInfo().number()
                                    ))
                                    .orElseThrow(IllegalStateException::new);

                            if (source.ref().repo().equals(snapshot.repo())) {
                                return Stream.empty();
                            }

                            return clones
                                    .stream()
                                    .filter(clone -> snapshot.pullInfo().equals(clone.ref().pullInfo()))
                                    .map(clone -> convert(source, clone));
                        }));

        return Mono
                .fromSupplier(cloneClassesSupplier)
                .flatMapMany(Flux::fromIterable);
    }

    private static CodeClone convert(final Clone<Snapshot> source, final Clone<Snapshot> target) {
        return CodeClone.builder()
                .source(convert(source))
                .target(convert(target))
                .build();
    }

    private static CodeClone.Snippet convert(final Clone<Snapshot> clone) {
        return CodeClone.Snippet.builder()
                .snapshot(clone.ref())
                .file(clone.filename())
                .method(clone.method())
                .lines(clone.lines())
                .build();
    }

    private static boolean cloneClassMatchesRules(final CloneClass<Snapshot> cloneClass, final Config rules) {
        return cloneClass.length() >= rules.cloneMinTokenCount()
               && cloneClass
                       .clones()
                       .stream()
                       .allMatch(clone -> rules.filter().test(clone.start().filename()));
    }

    private static boolean cloneClassContainsClonesForSnapshot(final CloneClass<Snapshot> cloneClass, final Snapshot thisPullSnapshot) {
        final var clones = cloneClass.clones();
        var containsCloneFromThisPull = false;
        var containsCloneFromOtherRepo = false;
        final var cloneCount = clones.size();
        for (int i = 0; i < cloneCount; ++i) {
            final var clone = clones.get(i);
            if (clone.ref().pullInfo().id().equals(thisPullSnapshot.pullInfo().id())) {
                containsCloneFromThisPull = true;
            }
            if (!clone.ref().repo().equals(thisPullSnapshot.repo())) {
                containsCloneFromOtherRepo = true;
            }
            if (containsCloneFromThisPull && containsCloneFromOtherRepo) {
                return true;
            }
        }
        return false;
    }
}
