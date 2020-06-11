package org.accula.api.detector;

import org.accula.api.code.CodeLoader;
import org.accula.api.code.CodeLoaderImpl;
import org.accula.api.code.FileEntity;
import org.accula.api.code.FileFilter;
import org.accula.api.code.RepositoryManager;
import org.accula.api.code.RepositoryProvider;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vadim Dyachkov
 */
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
        var repoOwner = new GithubUser(1L, "owner", "owner", "ava", false);
        GithubRepo repo = new GithubRepo(1L, "repo", "descr", repoOwner);
        CommitSnapshot commitSnapshot = CommitSnapshot.builder().sha("sha").branch("branch").repo(repo).build();
        FileEntity target1 = new FileEntity(commitSnapshot, "01.txt", "4\n6\n7\n8\n9\n\n\n");
        FileEntity target2 = new FileEntity(commitSnapshot, "02.txt", "10\n11\n1\n2\n");

        // source files
        var repoOwner1 = new GithubUser(2L, "owner1", "owner", "ava", false);
        GithubRepo repo1 = new GithubRepo(2L, "repo1", "descr", repoOwner1);
        CommitSnapshot commitSnapshot1 = CommitSnapshot.builder().sha("sha1").branch("branch").repo(repo1).build();
        FileEntity source1 = new FileEntity(commitSnapshot1, "1.txt", "1\n2\n3\n4\n9\n");

        var repoOwner2 = new GithubUser(3L, "owner2", "owner", "ava", false);
        GithubRepo repo2 = new GithubRepo(3L, "repo2", "descr", repoOwner2);
        CommitSnapshot commitSnapshot2 = CommitSnapshot.builder().sha("sha2").branch("branch").repo(repo2).build();
        FileEntity source2 = new FileEntity(commitSnapshot2, "2.txt", target1.getContent());

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

        var repoOwner = new GithubUser(1L, "vaddya", "owner", "ava", false);
        GithubRepo repo = new GithubRepo(1L, "2017-highload-kv", "descr", repoOwner);
        CommitSnapshot commitSnapshot = CommitSnapshot.builder().sha("076c99d7bbb06b31c27a9c3164f152d5c18c5010").branch("branch").repo(repo).build();
        Flux<FileEntity> targetFiles = codeLoader.getFiles(commitSnapshot, FileFilter.SRC_JAVA);

        var repoOwner1 = new GithubUser(2L, "lamtev", "owner", "ava", false);
        GithubRepo repo1 = new GithubRepo(2L, "2017-highload-kv", "descr", repoOwner1);
        CommitSnapshot commitSnapshot1 = CommitSnapshot.builder().sha("8ad07b914c0c2cee8b5a47993061b79c611db65d").branch("branch").repo(repo1).build();
        Flux<FileEntity> sourceFiles = codeLoader.getFiles(commitSnapshot1, FileFilter.SRC_JAVA);

        CloneDetector detector = new PrimitiveCloneDetector(10, 5);
        List<Tuple2<CodeSnippet, CodeSnippet>> clones = detector.findClones(targetFiles, sourceFiles).collectList().block();
        assertNotNull(clones);
        clones.forEach(t -> printClone(codeLoader, t));
        assertEquals(5, clones.size());
    }

    private void printClone(CodeLoader codeLoader, Tuple2<CodeSnippet, CodeSnippet> clone) {
        CodeSnippet into = clone.getT1();
        CodeSnippet from = clone.getT2();
        FileEntity fromCode = codeLoader.getFileSnippet(from.getCommitSnapshot(), from.getFile(), from.getFromLine(), from.getToLine()).block();
        assertNotNull(fromCode);
        System.out.println(into + " -> " + from);
        System.out.println(fromCode.getContent());
        System.out.println("======================");
    }
}
