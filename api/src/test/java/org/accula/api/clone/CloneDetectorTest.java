package org.accula.api.clone;

import org.accula.api.code.FileEntity;
import org.accula.api.code.FileFilter;
import org.accula.api.code.lines.LineSet;
import org.accula.api.db.model.CodeLanguage;
import org.accula.api.db.model.Snapshot;
import org.accula.api.token.java.JavaTokenProviderTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;

import static org.accula.api.util.TestData.accula485b362Snap;
import static org.accula.api.util.TestData.highload2019_174Head;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
class CloneDetectorTest {
    public static final Snapshot commitSnapshot1 = highload2019_174Head.withPullInfo(Snapshot.PullInfo.of(1L, 2));
    public static final Snapshot commitSnapshot2 = highload2019_174Head.withPullInfo(Snapshot.PullInfo.of(1L, 3));
    public static final Snapshot commitSnapshot3 = accula485b362Snap.withPullInfo(Snapshot.PullInfo.of(2L, 3));
    public static final FileEntity<Snapshot> source1 = new FileEntity<>(commitSnapshot1, "owner1/repo1/src/main/java/Cell.java", JavaTokenProviderTest.content1, LineSet.all());
    public static final FileEntity<Snapshot> source2 = new FileEntity<>(commitSnapshot2, "owner1/repo1/src/main/java/Cell.java", JavaTokenProviderTest.content1, LineSet.all());
    public static final FileEntity<Snapshot> target1 = new FileEntity<>(commitSnapshot3, "owner2/repo2/src/main/java/Cell.java", JavaTokenProviderTest.content2, LineSet.all());

    CloneDetector cloneDetector;

    @BeforeEach
    void setUp() {
        cloneDetector = new CloneDetectorImpl(() -> Mono.just(CloneDetector.Config.builder()
                .cloneMinTokenCount(5)
                .filter(FileFilter.notIn(Set.of("other/guy/src/main/java/Cell.java")))
                .language(CodeLanguage.JAVA)
                .language(CodeLanguage.KOTLIN)
                .build()));
    }

    @Test
    void test() {
        StepVerifier.create(cloneDetector.fill(Flux.just(source1, source2, target1)))
                .verifyComplete();

        StepVerifier.create(cloneDetector.readClones(commitSnapshot3)
                .collectList())
                .expectNextMatches(clones -> {
                    if (clones.size() != 5) {
                        System.err.println("Actual size = " + clones.size());
                        return false;
                    }
                    return clones.stream().allMatch(clone -> clone.source().snapshot().equals(commitSnapshot1));
                })
                .verifyComplete();
    }

    @Test
    void testNoTokenProviders() {
        final var cloneDetector = new CloneDetectorImpl(() -> Mono.just(CloneDetector.Config.builder()
            .cloneMinTokenCount(5)
            .filter(FileFilter.notIn(Set.of("other/guy/src/main/java/Cell.java")))
            .languages(List.of())
            .build()));

        cloneDetector.fill(Flux.just(source1, target1))
            .as(StepVerifier::create)
            .verifyComplete();
        cloneDetector.readClones(commitSnapshot3)
            .as(StepVerifier::create)
            .verifyComplete();
    }
}
