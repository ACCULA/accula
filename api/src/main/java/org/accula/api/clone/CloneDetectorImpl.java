package org.accula.api.clone;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.clone.suffixtree.Clone;
import org.accula.api.clone.suffixtree.CloneClass;
import org.accula.api.clone.suffixtree.SuffixTreeCloneDetector;
import org.accula.api.code.FileEntity;
import org.accula.api.code.FileFilter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Snapshot;
import org.accula.api.token.Token;
import org.accula.api.token.TokenProvider;
import org.accula.api.token.java.JavaTokenProvider;
import org.accula.api.token.kotlin.KotlinTokenProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.function.BinaryOperator.minBy;

/**
 * @author Anton Lamtev
 */
@Slf4j
public final class CloneDetectorImpl implements CloneDetector {
    private static final int CHEAP_CHECK_CLONE_COUNT_THRESHOLD = 10;
    private final GithubRepo.Identity projectId;
    //FIXME: avoid blocking
    private final SuffixTreeCloneDetector<Token<Snapshot>, Snapshot> suffixTreeCloneDetector;
    private final ConfigProvider configProvider;

    public CloneDetectorImpl(final GithubRepo.Identity projectId, final ConfigProvider configProvider) {
        this.projectId = projectId;
        this.suffixTreeCloneDetector = new SuffixTreeCloneDetector<>(projectId.toString());
        this.configProvider = configProvider;
    }

    @Override
    public Flux<CodeClone> readClones(final Snapshot pullSnapshot) {
        final Predicate<Clone<Snapshot>> cloneMatcher = clone -> pullSnapshot.pullInfo().equals(clone.ref().pullInfo());
        return configProvider
            .get()
            .flatMapMany(config -> readClonesFromSuffixTreeForPull(pullSnapshot, config, cloneMatcher));
    }

    @Override
    public Flux<CodeClone> findClones(final Snapshot pullSnapshot,
                                      final Flux<FileEntity<Snapshot>> files,
                                      final Iterable<Snapshot> snapshots) {
        final var snapshotSet = snapshots instanceof Set<Snapshot> s ? s : Sets.newHashSet(snapshots);
        final Predicate<Clone<Snapshot>> cloneMatcher = clone -> snapshotSet.contains(clone.ref());
        return addFilesToSuffixTree(files)
            .then(configProvider.get())
            .flatMapMany(config -> readClonesFromSuffixTreeForPull(pullSnapshot, config, cloneMatcher));
    }

    @Override
    public Mono<Void> fill(final Flux<FileEntity<Snapshot>> files) {
        return addFilesToSuffixTree(files);
    }

    private Mono<Void> addFilesToSuffixTree(final Flux<FileEntity<Snapshot>> files) {
        return tokensByMethods(files)
                .flatMap(method ->
                        Mono.fromSupplier(() -> suffixTreeCloneDetector.addTokens(method)))
                .then();
    }

    private Flux<List<Token<Snapshot>>> tokensByMethods(final Flux<FileEntity<Snapshot>> files) {
        return configProvider
            .get()
            .flatMapMany(config -> {
                final var tokenProviders = config
                    .languages()
                    .stream()
                    .map(language -> switch (language) {
                        case JAVA -> new JavaTokenProvider<Snapshot>();
                        case KOTLIN -> new KotlinTokenProvider<Snapshot>();
                    })
                    .toList();
                if (tokenProviders.isEmpty()) {
                    log.warn("No token providers configured");
                    return Mono.empty();
                }
                return new TokenProvider<>(tokenProviders).tokensByMethods(files)
                    .filter(methodTokens -> methodTokens.size() >= config.cloneMinTokenCount());
            });
    }

    private Flux<CodeClone> readClonesFromSuffixTreeForPull(final Snapshot pullSnapshot,
                                                            final Config config,
                                                            final Predicate<Clone<Snapshot>> cloneMatcher) {
        return Flux.fromStream(() -> suffixTreeCloneDetector
            .cloneClasses(cloneClass -> isGoodCloneClassForPullCheapCheck(cloneClass, pullSnapshot, config.cloneMinTokenCount()),
                          cloneClass -> isGoodCloneClassForPullExpensiveCheck(cloneClass, pullSnapshot, config.filter(), projectId))
            .stream()
            .flatMap(cloneClass -> {
                final var clones = cloneClass.clones();
                final var source = clones
                    .stream()
                    .reduce(minBy(
                        Comparator.comparing((Clone<Snapshot> clone) -> clone.ref().commit().date())
                            .thenComparing(clone -> clone.ref().pullInfo() != null ? clone.ref().pullInfo().number() : 0)
                    ))
                    .orElseThrow(IllegalStateException::new);

                if (source.ref().sha().equals(pullSnapshot.sha())) {
                    return Stream.empty();
                }
                if (source.ref().repo().equals(pullSnapshot.repo())) {
                    return Stream.empty();
                }
                if (config.excludedSourceAuthors().test(source.ref().repo().owner().id())) {
                    return Stream.empty();
                }

                return clones
                    .stream()
                    .filter(clone -> authorIsDifferentFromSource(clone, source))
                    .filter(cloneMatcher)
                    .map(clone -> convert(source, clone));
            }))
            .subscribeOn(Schedulers.parallel());
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

    /**
     * We accept clone class if its token count is larger than or equal to specified {@code cloneMinTokenCount}.
     * And for clone classes with a small clone count (<= {@code CHEAP_CHECK_CLONE_COUNT_THRESHOLD})
     * we do an additional checks:
     * <p>
     * 1. If at least one clone is linked to the {@code pullSnapshot}, we assume clone class is ok;
     * <p>
     * 2. If all clones have same author then the clone class is considered to be not ok.
     */
    private static boolean isGoodCloneClassForPullCheapCheck(final CloneClass<Snapshot> cloneClass,
                                                             final Snapshot pullSnapshot,
                                                             final int cloneMinTokenCount) {
        if (cloneClass.length() < cloneMinTokenCount) {
            return false;
        }
        final var clones = cloneClass.clones();
        final var cloneCount = clones.size();
        if (cloneCount <= CHEAP_CHECK_CLONE_COUNT_THRESHOLD) {
            for (int i = 0; i < cloneCount; ++i) {
                final var current = clones.get(i).ref();
                if (Objects.equals(current.pullInfo(), pullSnapshot.pullInfo())) {
                    return true;
                }
                if (i < 1) {
                    continue;
                }
                final var previous = clones.get(i - 1).ref();
                if (!current.repo().owner().equals(previous.repo().owner())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * We accept clone class if all its clones filenames match {@code fileFilter}
     * and all its clones repos differ from project's repo (? to be reviewed ?) and
     * both at least one clone pull matches specified one (via {@code pullSnapshot}) and
     * at least one clone repo is differ than repo of the specified {@code pullSnapshot}
     */
    private static boolean isGoodCloneClassForPullExpensiveCheck(final CloneClass<Snapshot> cloneClass,
                                                                 final Snapshot pullSnapshot,
                                                                 final FileFilter fileFilter,
                                                                 final GithubRepo.Identity projectId) {
        final var clones = cloneClass.clones();
        final var cloneCount = clones.size();
        var containsCloneFromThisPull = false;
        var containsCloneFromOtherRepo = false;
        for (int i = 0; i < cloneCount; ++i) {
            final var clone = clones.get(i);
            if (!fileFilter.test(clone.start().filename())) {
                return false;
            }
            final var cloneSnapshot = clone.ref();
            // Idea was in that if clone repo is equal to project repo
            // then it's contributed code to project repo and is not clone at all.
            // But it was actual when students solution was not merging to upstream.
            // TODO: review the approach
            if (Objects.equals(cloneSnapshot.repo().identity(), projectId)) {
                return false;
            }
            if (Objects.equals(cloneSnapshot.pullInfo(), pullSnapshot.pullInfo())) {
                containsCloneFromThisPull = true;
            }
            if (!cloneSnapshot.repo().equals(pullSnapshot.repo())) {
                containsCloneFromOtherRepo = true;
            }
            if (containsCloneFromThisPull && containsCloneFromOtherRepo) {
                return true;
            }
        }
        return false;
    }

    private static boolean authorIsDifferentFromSource(final Clone<Snapshot> possibleClone, final Clone<Snapshot> source) {
        final var cloneSnapshot = possibleClone.ref();
        final var sourceSnapshot = source.ref();
        if (cloneSnapshot.repo().owner().equals(sourceSnapshot.repo().owner())) {
            return false;
        }
        if (cloneSnapshot.commit().sha().equals(sourceSnapshot.commit().sha())) {
            return false;
        }
        return !cloneSnapshot.commit().authorEmail().equals(sourceSnapshot.commit().authorEmail());
    }
}
