package org.accula.api.detector;

import lombok.SneakyThrows;
import org.accula.api.code.FileEntity;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vadim Dyachkov
 */
class CloneDetectorTest {
    /**
     * owner/repo/sha:Main.txt[4:8] -> owner1/repo1/sha1:Common.txt[6:10]
     * owner/repo/sha:Main.txt[4:8] -> owner1/repo1/sha1:Common.txt[14:15]
     * owner/repo/sha:Main.txt[4:8] -> owner1/repo1/sha1:Common.txt[24:31]
     * owner/repo/sha:Main.txt[4:8] -> owner1/repo1/sha1:Main.txt[5:9]
     * owner/repo/sha:Main.txt[4:8] -> owner2/repo2/sha2:Task.txt[10:14]
     * owner/repo/sha:Main.txt[4:8] -> owner2/repo2/sha2:Task.txt[15:19]
     * owner/repo/sha:Main.txt[4:8] -> owner1/repo1/sha1:Code.txt[10:14]
     * owner/repo/sha:Main2.txt[7:15] -> owner2/repo2/sha2:Task.txt[24:27]
     * Second run
     * owner/repo/sha:Main.txt[4:8] -> owner1/repo1/sha1:Common.txt[6:10]
     * owner/repo/sha:Main.txt[4:8] -> owner1/repo1/sha1:Common.txt[14:15]
     * owner/repo/sha:Main.txt[4:8] -> owner1/repo1/sha1:Common.txt[24:31]
     */
    @Test
    void testSuffixTreeDetector() {
        CloneDetector detector = new CloneDetectorImpl(() -> Mono.just(CloneDetector.Config.builder().minCloneLength(3).build()));

        //@formatter:off
        var repoOwner       = new GithubUser(1L, "owner", "owner", "ava", false);
        var repoOwner1      = new GithubUser(2L, "owner1", "owner", "ava", false);
        var repoOwner2      = new GithubUser(3L, "owner2", "owner", "ava", false);
        var repo            = new GithubRepo(1L, "repo", "descr", repoOwner);
        var repo1           = new GithubRepo(2L, "repo1", "descr", repoOwner1);
        var repo2           = new GithubRepo(3L, "repo2", "descr", repoOwner2);
        var commitSnapshot  = CommitSnapshot.builder().sha("sha").branch("branch").repo(repo).build();
        var commitSnapshot1 = CommitSnapshot.builder().sha("sha1").branch("branch").repo(repo1).build();
        var commitSnapshot2 = CommitSnapshot.builder().sha("sha2").branch("branch").repo(repo2).build();
        //@formatter:on

        // target files
        var target1 = new FileEntity(commitSnapshot2, "Main.txt", content("target/Main.txt"));
        var target2 = new FileEntity(commitSnapshot2, "Main2.txt", content("target/Main2.txt"));

        // source files
        var source1 = new FileEntity(commitSnapshot1, "Common.txt", content("source/Common.txt"));
        var source2 = new FileEntity(commitSnapshot1, "Main.txt", content("source/Main.txt"));
        var source3 = new FileEntity(commitSnapshot1, "Task.txt", content("source/Task.txt"));
        var source4 = new FileEntity(commitSnapshot1, "Code.txt", content("source/Code.txt"));

        var trg1 = new CodeSnippet(target1.getCommitSnapshot(), target1.getName(), 4, 8);
        var trg2 = new CodeSnippet(target2.getCommitSnapshot(), target2.getName(), 7, 15);

        //expected result on first execution
        Supplier<Stream<Tuple2<CodeSnippet, CodeSnippet>>> expectedPairs = () -> Stream.of(
                Tuples.of(trg1, new CodeSnippet(source1.getCommitSnapshot(), source1.getName(), 6, 10)),
                Tuples.of(trg1, new CodeSnippet(source1.getCommitSnapshot(), source1.getName(), 14, 15)),
                Tuples.of(trg1, new CodeSnippet(source1.getCommitSnapshot(), source1.getName(), 24, 31)),
                Tuples.of(trg1, new CodeSnippet(source2.getCommitSnapshot(), source2.getName(), 5, 9)),
                Tuples.of(trg1, new CodeSnippet(source3.getCommitSnapshot(), source3.getName(), 10, 14)),
                Tuples.of(trg1, new CodeSnippet(source3.getCommitSnapshot(), source3.getName(), 15, 19)),
                Tuples.of(trg1, new CodeSnippet(source4.getCommitSnapshot(), source4.getName(), 10, 14)),
                Tuples.of(trg2, new CodeSnippet(source3.getCommitSnapshot(), source3.getName(), 24, 27)));

        // expected result on second execution
        var result2 = expectedPairs
                .get()
                .limit(3)
                .collect(Collectors.toSet());

        //First execution
        Flux<FileEntity> target = Flux.just(target1, target2);
        Flux<FileEntity> source = Flux.just(source1, source2, source3, source4);

        detector.fill(source).block();
        List<Tuple2<CodeSnippet, CodeSnippet>> clones = detector.findClones(target1.getCommitSnapshot(), target).collectList().block();
        assert clones != null;
        assertEquals(expectedPairs.get().collect(Collectors.toSet()), Set.copyOf(clones));
        clones.forEach(t -> System.out.println(t.getT1() + " -> " + t.getT2()));

        //Second execution - test that suffix tree is empty after first execution
        detector = new CloneDetectorImpl(() -> Mono.just(CloneDetector.Config.builder().minCloneLength(3).build()));
        System.out.println("Second run");
        Flux<FileEntity> targets = Flux.just(target1);
        Flux<FileEntity> sources = Flux.just(source1);
        detector.fill(sources).block();
        List<Tuple2<CodeSnippet, CodeSnippet>> clones2 = detector.findClones(target1.getCommitSnapshot(), targets).collectList().block();
        assert clones2 != null;
        assertEquals(result2, Set.copyOf(clones2));
        clones2.forEach(t -> System.out.println(t.getT1() + " -> " + t.getT2()));
    }

    @SneakyThrows
    private static String content(String pathStr) {
        String testFilesDir = "src/test/resources/testfiles/";
        Path path = Path.of(testFilesDir, pathStr);
        return new String(Files.readAllBytes(path));
    }
}
