package org.accula.api.detector;

import org.accula.api.code.CommitMarker;
import org.accula.api.code.FileEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.util.List;

class CloneDetectorTest {

    private CloneDetector detector = new CloneDetectorImpl();

    /**
     * owner3/repo3/sha3:3.txt[1:1] -> [owner1/repo1/sha1:1.txt[2:2]]
     * owner3/repo3/sha3:3.txt[2:2] -> [owner1/repo1/sha1:1.txt[4:4], owner2/repo2/sha2:2.txt[1:1]]
     */
    @Test
    void test() {
        CommitMarker commit1 = new CommitMarker("owner1", "repo1", "sha1");
        FileEntity file1 = new FileEntity(commit1, "1.txt", "1\n2\n3\n4");

        CommitMarker commit2 = new CommitMarker("owner2", "repo2", "sha2");
        FileEntity file2 = new FileEntity(commit2, "2.txt", "4\n5");

        CommitMarker commitToCheck1 = new CommitMarker("owner3", "repo3", "sha3");
        FileEntity fileToCheck1 = new FileEntity(commitToCheck1, "3.txt", "2\n4\n6");

        MultiValueMap<CodeSnippet, CodeSnippet> clones = detector.findClones(List.of(fileToCheck1), List.of(file1, file2));
        clones.forEach((k, v) -> System.out.println(k + " -> " + v));
        Assertions.assertEquals(2, clones.size());
    }
}