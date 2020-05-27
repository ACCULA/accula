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
        detector = new CloneDetectorImpl();
        CloneDetectorImpl.MIN_LINE_LENGTH = 1;
    }

    /**
     * owner3/repo3/sha3:3.txt[1:1] -> owner1/repo1/sha1:1.txt[2:2]
     * owner3/repo3/sha3:3.txt[2:2] -> owner1/repo1/sha1:1.txt[4:4]
     * owner3/repo3/sha3:3.txt[6:6] -> owner1/repo1/sha1:1.txt[5:5]
     * owner3/repo3/sha3:3.txt[2:2] -> owner2/repo2/sha2:2.txt[1:1]
     * owner3/repo3/sha3:3.txt[3:6] -> owner2/repo2/sha2:2.txt[4:7]
     */
    @Test
    void test() {
        CommitMarker commit1 = new CommitMarker("owner1", "repo1", "sha1");
        FileEntity file1 = new FileEntity(commit1, "1.txt", "1\n2\n3\n4\n9\n");

        CommitMarker commit2 = new CommitMarker("owner2", "repo2", "sha2");
        FileEntity file2 = new FileEntity(commit2, "2.txt", "4\n5\n0\n6\n7\n8\n9\n10\n\n");

        CommitMarker commitToCheck1 = new CommitMarker("owner3", "repo3", "sha3");
        FileEntity fileToCheck1 = new FileEntity(commitToCheck1, "3.txt", "2\n4\n6\n7\n8\n9\n\n\n");

        List<Tuple2<CodeSnippet, CodeSnippet>> clones = detector.findClones(Flux.just(fileToCheck1), Flux.just(file1, file2))
                .collectList().block();
        assert clones != null;
        clones.forEach(t -> System.out.println(t.getT1() + " -> " + t.getT2()));
        Assertions.assertEquals(5, clones.size());
    }
}
