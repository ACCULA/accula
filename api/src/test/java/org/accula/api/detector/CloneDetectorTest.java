package org.accula.api.detector;

import org.accula.api.code.*;
import org.accula.api.db.model.Commit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vadim Dyachkov
 */
//FIXME: replace block() with StepVerifier
class CloneDetectorTest {
    /**
     * owner/repo/sha:02.txt[3:4] -> owner1/repo1/sha1:1.txt[1:2]
     * owner/repo/sha:01.txt[1:1] -> owner1/repo1/sha1:1.txt[4:4]
     * owner/repo/sha:01.txt[5:5] -> owner1/repo1/sha1:1.txt[5:5]
     * owner/repo/sha:01.txt[1:5] -> owner2/repo2/sha2:2.txt[1:5]
     */
    @Test
    void testStubs() {
        CloneDetector detector = new PrimitiveCloneDetector(1, 1);

        // target file
        Commit commit = new Commit(0L, "owner", "repo", "sha");
        FileEntity target1 = new FileEntity(commit, "01.txt", "4\n6\n7\n8\n9\n\n\n");
        FileEntity target2 = new FileEntity(commit, "02.txt", "10\n11\n1\n2\n");

        // source files
        Commit commit1 = new Commit(1L, "owner1", "repo1", "sha1");
        FileEntity source1 = new FileEntity(commit1, "1.txt", "1\n2\n3\n4\n9\n");

        Commit commit2 = new Commit(2L, "owner2", "repo2", "sha2");
        FileEntity source2 = new FileEntity(commit2, "2.txt", target1.getContent());

        // find clones
        Flux<FileEntity> target = Flux.just(target1, target2);
        Flux<FileEntity> source = Flux.just(source1, source2);
        List<Tuple2<CodeSnippet, CodeSnippet>> clones = detector.findClones(target, source).collectList().block();
        assert clones != null;
        clones.forEach(t -> System.out.println(t.getT1() + " -> " + t.getT2()));
        assertEquals(4, clones.size());
    }

    @Test
    void testReal(@TempDir final File tempDir) {
        RepositoryProvider repositoryProvider = new RepositoryManager(tempDir);
        CodeLoader codeLoader = new CodeLoaderImpl(repositoryProvider);

        Commit targetMarker = new Commit(0L, "vaddya", "2017-highload-kv", "076c99d7bbb06b31c27a9c3164f152d5c18c5010");
        Flux<FileEntity> targetFiles = codeLoader.getFiles(targetMarker, FileFilter.SRC_JAVA);

        Commit sourceMarker = new Commit(1L, "lamtev", "2017-highload-kv", "8ad07b914c0c2cee8b5a47993061b79c611db65d");
        Flux<FileEntity> sourceFiles = codeLoader.getFiles(sourceMarker, FileFilter.SRC_JAVA);

        CloneDetector detector = new PrimitiveCloneDetector(10, 5);
        List<Tuple2<CodeSnippet, CodeSnippet>> clones = detector.findClones(targetFiles, sourceFiles).collectList().block();
        assert clones != null;
        clones.forEach(t -> printClone(codeLoader, t));
        assertEquals(5, clones.size());
    }

    private void printClone(CodeLoader codeLoader, Tuple2<CodeSnippet, CodeSnippet> clone) {
        CodeSnippet into = clone.getT1();
        CodeSnippet from = clone.getT2();

        FileSnippetMarker marker = new FileSnippetMarker(from.getCommit(), from.getFile(), from.getFromLine(), from.getToLine());
        String fromCode = codeLoader.getFileSnippet(marker).block();

        System.out.println(into + " -> " + from);
        System.out.println(fromCode);
        System.out.println("======================");
    }
}
