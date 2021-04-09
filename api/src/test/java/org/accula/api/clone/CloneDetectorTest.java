package org.accula.api.clone;

import org.accula.api.clone.suffixtree.SuffixTreeCloneDetectorTest;
import org.accula.api.code.FileEntity;
import org.accula.api.code.FileFilter;
import org.accula.api.code.lines.LineSet;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Snapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
class CloneDetectorTest {
    CloneDetector cloneDetector;

    @BeforeEach
    void setUp() {
        cloneDetector = new CloneDetectorImpl(() -> Mono.just(CloneDetector.Config.builder()
                .cloneMinTokenCount(5)
                .filter(FileFilter.exclude(Set.of("other/guy/src/main/java/Cell.java")))
                .build()));
    }

    @Test
    void test() {
        var repoOwner1 = new GithubUser(2L, "owner1", "owner1", "ava1", false);
        var repoOwner2 = new GithubUser(3L, "owner2", "owner2", "ava2", false);
        var repo1 = new GithubRepo(2L, "repo1", false, "descr1", repoOwner1);
        var repo2 = new GithubRepo(3L, "repo2", false, "descr2", repoOwner2);
        var commitSnapshot1 = Snapshot.builder()
                .commit(Commit.builder()
                        .date(Instant.EPOCH.plus(20L, ChronoUnit.MINUTES))
                        .sha("sha1")
                        .authorName("name 1")
                        .authorEmail("e@mail.org")
                        .build())
                .branch("branch1")
                .repo(repo1)
                .pullInfo(Snapshot.PullInfo.of(1L, 2))
                .build();
        var commitSnapshot2 = Snapshot.builder()
                .commit(Commit.builder()
                        .date(Instant.now())
                        .sha("sha2")
                        .authorName("name 2")
                        .authorEmail("e2@mail.org")
                        .build())
                .branch("branch2")
                .pullInfo(Snapshot.PullInfo.of(2L, 3))
                .repo(repo2)
                .build();

        var source1 = new FileEntity<>(commitSnapshot1, "owner1/repo1/src/main/java/Cell.java", SuffixTreeCloneDetectorTest.F1, LineSet.all());
        var target1 = new FileEntity<>(commitSnapshot2, "owner2/repo2/src/main/java/Cell.java", SuffixTreeCloneDetectorTest.F2, LineSet.all());

        StepVerifier.create(cloneDetector.fill(Flux.just(source1, target1)))
                .verifyComplete();

        StepVerifier.create(cloneDetector.readClones(commitSnapshot2)
                .collectList())
                .expectNextMatches(clones -> {
                    if (clones.size() != 3) {
                        System.err.println("Actual size = " + clones.size());
                        return false;
                    }
                    return clones.stream().allMatch(clone -> clone.source().snapshot().equals(commitSnapshot1));
                })
                .verifyComplete();
    }
}
