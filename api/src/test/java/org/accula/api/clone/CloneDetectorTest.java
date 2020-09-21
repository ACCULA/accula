package org.accula.api.clone;

import org.accula.api.clone.suffixtree.SuffixTreeCloneDetectorTest;
import org.accula.api.code.FileEntity;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.Set;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
class CloneDetectorTest {
    CloneDetector cloneDetector;

    @BeforeEach
    void setUp() {
        cloneDetector = new CloneDetectorImpl(() -> Mono.just(CloneDetector.Config.builder().minCloneLength(1).build()));
    }

    @Test
    void test() {
        var repoOwner1 = new GithubUser(2L, "owner1", "owner1", "ava1", false);
        var repoOwner2 = new GithubUser(3L, "owner2", "owner2", "ava2", false);
        var repo1 = new GithubRepo(2L, "repo1", "descr1", repoOwner1);
        var repo2 = new GithubRepo(3L, "repo2", "descr2", repoOwner2);
        var commitSnapshot1 = CommitSnapshot.builder().sha("sha1").branch("branch1").repo(repo1).build();
        var commitSnapshot2 = CommitSnapshot.builder().sha("sha2").branch("branch2").repo(repo2).build();

        var source1 = new FileEntity<>(commitSnapshot1, "owner1/repo1/src/main/java/Cell.java", SuffixTreeCloneDetectorTest.F1);
        var target1 = new FileEntity<>(commitSnapshot2, "owner2/repo2/src/main/java/Cell.java", SuffixTreeCloneDetectorTest.F2);
        var source2 = new FileEntity<>(commitSnapshot1, "owner1/repo1/src/main/java/SSTable.java", SuffixTreeCloneDetectorTest.F3);
        var target2 = new FileEntity<>(commitSnapshot2, "owner2/repo2/src/main/java/SSTable.java", SuffixTreeCloneDetectorTest.F3);

        var resultSet = Set.of(
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target2.getName(), 5, 19),
                        new CodeSnippet(commitSnapshot1, source2.getName(), 5, 19)
                ),
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target2.getName(), 7, 19),
                        new CodeSnippet(commitSnapshot1, source2.getName(), 7, 19)
                ),
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target2.getName(), 3, 19),
                        new CodeSnippet(commitSnapshot1, source2.getName(), 3, 19)
                ),
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target2.getName(), 10, 19),
                        new CodeSnippet(commitSnapshot1, source2.getName(), 10, 19)
                ),
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target2.getName(), 12, 19),
                        new CodeSnippet(commitSnapshot1, source2.getName(), 12, 19)
                ),
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target2.getName(), 14, 19),
                        new CodeSnippet(commitSnapshot1, source2.getName(), 14, 19)
                ),
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target2.getName(), 8, 19),
                        new CodeSnippet(commitSnapshot1, source2.getName(), 8, 19)
                ),
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target2.getName(), 16, 19),
                        new CodeSnippet(commitSnapshot1, source2.getName(), 16, 19)
                ),
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target1.getName(), 5, 6),
                        new CodeSnippet(commitSnapshot1, source1.getName(), 3, 4)
                ),
                Tuples.of(
                        new CodeSnippet(commitSnapshot2, target1.getName(), 9, 9),
                        new CodeSnippet(commitSnapshot1, source1.getName(), 12, 12)
                )
        );

        StepVerifier.create(cloneDetector.fill(Flux.just(source1, source2)))
                .verifyComplete();

        StepVerifier.create(cloneDetector.findClones(commitSnapshot2, Flux.just(target1, target2))
                .collectList())
                .expectNextMatches(clones -> {
                    if (clones.size() != 10) {
                        System.err.println("Actual size = " + clones.size());
                        return false;
                    }
                    return resultSet.containsAll(clones);
                })
                .verifyComplete();
    }
}
