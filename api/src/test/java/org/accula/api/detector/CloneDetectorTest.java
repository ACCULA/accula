package org.accula.api.detector;

import org.accula.api.code.CommitMarker;
import org.accula.api.code.FileEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.util.List;

class CloneDetectorTest {
    private CloneDetector detector;

    @BeforeEach
    void setUp() {
        this.detector = new PrimitiveCloneDetector();
        PrimitiveCloneDetector.MIN_LINE_LENGTH = 1;
    }

    /**
     * owner/repo/sha:02.txt[3:4] -> owner1/repo1/sha1:1.txt[1:2]
     * owner/repo/sha:01.txt[1:1] -> owner1/repo1/sha1:1.txt[4:4]
     * owner/repo/sha:01.txt[5:5] -> owner1/repo1/sha1:1.txt[5:5]
     * owner/repo/sha:01.txt[1:5] -> owner2/repo2/sha2:2.txt[1:5]
     */
    @Test
    void test() {
        // target file
        CommitMarker commit = new CommitMarker("owner", "repo", "sha");
        FileEntity target1 = new FileEntity(commit, "01.txt", "4\n6\n7\n8\n9\n\n\n");
        FileEntity target2 = new FileEntity(commit, "02.txt", "10\n11\n1\n2\n");

        // source files
        CommitMarker commit1 = new CommitMarker("owner1", "repo1", "sha1");
        FileEntity source1 = new FileEntity(commit1, "1.txt", "1\n2\n3\n4\n9\n");

        CommitMarker commit2 = new CommitMarker("owner2", "repo2", "sha2");
        FileEntity source2 = new FileEntity(commit2, "2.txt", target1.getContent());

        // find clones
        Flux<FileEntity> target = Flux.just(target1, target2);
        Flux<FileEntity> source = Flux.just(source1, source2);
        List<Tuple2<CodeSnippet, CodeSnippet>> clones = detector.findClones(target, source).collectList().block();
        assert clones != null;
        clones.forEach(t -> System.out.println(t.getT1() + " -> " + t.getT2()));
        Assertions.assertEquals(4, clones.size());
    }
}
