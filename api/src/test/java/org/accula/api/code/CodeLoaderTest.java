package org.accula.api.code;

import org.accula.api.code.git.Git;
import org.accula.api.code.lines.LineRange;
import org.accula.api.code.lines.LineSet;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Snapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FIXME: replace block with StepVerifier
 *
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
class CodeLoaderTest {
    public static final String README = "README.md";
    public static final String CLUSTER_JAVA = "src/main/java/ru/mail/polis/Cluster.java";
    public static final GithubUser USER = GithubUser.builder().id(0L).login("polis-mail-ru").name("name").avatar("ava").isOrganization(true).build();
    public static final GithubRepo REPO = GithubRepo.builder().id(0L).name("2019-highload-dht").isPrivate(false).description("descr").owner(USER).build();
    public static final Snapshot COMMIT = Snapshot.builder()
            .commit(Commit.shaOnly("720cefb3f361895e9e23524c2b4025f9a949d5d2"))
            .branch("branch")
            .repo(REPO)
            .build();
    static final GithubUser lamtev = GithubUser.builder().id(0L).login("lamtev").avatar("").isOrganization(false).build();
    static final GithubUser vaddya = GithubUser.builder().id(0L).login("vaddya").avatar("").isOrganization(false).build();

    CodeLoader codeLoader;

    @BeforeEach
    void beforeAll(@TempDir final Path tempDir) {
        codeLoader = new GitCodeLoader(null, new Git(tempDir, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())));
    }

    @Test
    void testLoadFiles() {
        StepVerifier.create(codeLoader.loadFiles(COMMIT.repo(), List.of(COMMIT), README::equals))
                .expectNextMatches(readme -> readme.lines().equals(LineSet.of(LineRange.of(173), LineRange.of(194))))
                .expectComplete()
                .verify();
    }

    @Test
    void testGetMultipleFiles() {
        var repo = GithubRepo.builder().id(1L).name("2017-highload-kv").isPrivate(false).description("descr").owner(lamtev).build();
        final var s1 = Snapshot
                .builder()
                .commit(Commit.shaOnly("ecb40217f36891809693e4d9d37a3e841ff740b9"))
                .repo(repo)
                .pullInfo(Snapshot.PullInfo.of(10L, 7))
                .build();
        final var s2 = s1.withCommit(Commit.shaOnly("5d66d3b0c3f07c07eb841b1620dcba2b0a970bc7"))
                .withPullInfo(Snapshot.PullInfo.of(11L, 10));
        final var s3 = s1.withCommit(Commit.shaOnly("38f07e6b48c6594d9b3cfaa64b76a9aecc811b25"))
                .withPullInfo(Snapshot.PullInfo.of(12L, 11));
        final var s4 = s3.withCommit(Commit.shaOnly("38f07e6b48c6594d9b3cfaa64b76a9aecc811b25"))
                .withPullInfo(Snapshot.PullInfo.of(13L, 12));
        final var s5 = s3.withCommit(Commit.shaOnly("38f07e6b48c6594d9b3cfaa64b76a9aecc811b25"))
                .withPullInfo(Snapshot.PullInfo.of(14L, 13));
        var files = codeLoader.loadFiles(s1.repo(), List.of(s1, s2, s5, s3, s1, s4), FileFilter.SRC_JAVA)
                .collectList().block();
        assertNotNull(files);
        assertEquals(19, files.size());
        assertEquals(4L, files.stream().map(FileEntity::name).distinct().count());
    }

    @Test
    void testGetFileSnippetSingleLine() {
        var snippet = codeLoader.loadSnippets(COMMIT, List.of(SnippetMarker.of(README, LineRange.of(4, 4)))).blockFirst();
        assertNotNull(snippet);
        assertEquals("""
                ## Этап 1. HTTP + storage (deadline 2019-10-05)
                """, snippet.content());
    }

    @Test
    void testGetFileSnippetMultipleLines() {
        StepVerifier.create(codeLoader.loadSnippets(COMMIT, List.of(
                SnippetMarker.of(README, LineRange.of(4, 5)),
                SnippetMarker.of("NOT_EXISTENT_FILE", LineRange.of(1, 1))))
                .map(FileEntity::content))
                .expectNextMatches(content -> content.equals("""
                        ## Этап 1. HTTP + storage (deadline 2019-10-05)
                        ### Fork
                        """))
                .expectComplete()
                .verify();
    }

    @Test
    void testLoadManySnippetsFromSameFile() {
        final var snippets = codeLoader
                .loadSnippets(COMMIT, List.of(
                        SnippetMarker.of(README, LineRange.of(1, 5)),
                        SnippetMarker.of(README, LineRange.of(4, 10)),
                        SnippetMarker.of(CLUSTER_JAVA, LineRange.of(38, 39)),
                        SnippetMarker.of(README, LineRange.of(7, 15)),
                        SnippetMarker.of(CLUSTER_JAVA, LineRange.of(51, 56))
                ))
                .collectList()
                .block();
        assertNotNull(snippets);
        assertEquals(5, snippets.size());
        assertEquals("""
                # 2019-highload-dht
                Курсовой проект 2019 года [курса](https://polis.mail.ru/curriculum/program/discipline/792/) "Highload системы" в [Технополис](https://polis.mail.ru).
                                
                ## Этап 1. HTTP + storage (deadline 2019-10-05)
                ### Fork
                """, snippets.get(0).content());
        assertEquals("""
                ## Этап 1. HTTP + storage (deadline 2019-10-05)
                ### Fork
                [Форкните проект](https://help.github.com/articles/fork-a-repo/), склонируйте и добавьте `upstream`:
                ```
                $ git clone git@github.com:<username>/2019-highload-dht.git
                Cloning into '2019-highload-dht'...
                ...
                """, snippets.get(1).content());
        assertEquals("""
                    private static final int[] PORTS = {8080, 8081, 8082};
                    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
                """, snippets.get(2).content());
        assertEquals("""
                ```
                $ git clone git@github.com:<username>/2019-highload-dht.git
                Cloning into '2019-highload-dht'...
                ...
                $ git remote add upstream git@github.com:polis-mail-ru/2019-highload-dht.git
                $ git fetch upstream
                From github.com:polis-mail-ru/2019-highload-dht
                 * [new branch]      master     -> upstream/master
                ```
                """, snippets.get(3).content());
        assertEquals("""
                    public static void main(final String[] args) throws IOException {
                        // Fill the topology
                        final Set<String> topology = new HashSet<>(3);
                        for (final int port : PORTS) {
                            topology.add("http://localhost:" + port);
                        }
                """, snippets.get(4).content());
    }

    @Test
    void testDiff() {
        var headOwner = GithubUser.builder().id(1L).login("vaddya").name("owner").avatar("ava").isOrganization(false).build();
        var headRepo = GithubRepo.builder().id(1L).name("2019-highload-dht").isPrivate(false).description("descr").owner(headOwner).build();
        var head = Snapshot.builder().commit(Commit.shaOnly("a1c28a1b500701819cf9919246f15f3f900bb609")).branch("branch").repo(headRepo).build();
        var base = Snapshot.builder().commit(Commit.shaOnly("d6357dccc16c7d5c001fd2a2203298c36fe96b63")).branch("branch").repo(REPO).build();
        StepVerifier.create(codeLoader.loadDiff(base, head, 0, FileFilter.SRC_JAVA))
                .expectNextCount(11)
                .expectComplete()
                .verify();
    }

    @Test
    void testRemoteDiff() {
        var projectRepo = GithubRepo.builder().id(1L).name("2017-highload-kv").isPrivate(false).description("descr").owner(USER).build();
        var baseRepo = GithubRepo.builder().id(0L).name("2017-highload-kv").isPrivate(false).description("").owner(lamtev).build();
        var base = Snapshot
                .builder()
                .commit(Commit.shaOnly("fe675f17ad4aab9a8c853b5f3b07b0bc64f06907"))
                .repo(baseRepo)
                .build();
        var headRepo = GithubRepo.builder().id(0L).name("2017-highload-kv").isPrivate(false).description("").owner(vaddya).build();
        var head = Snapshot
                .builder()
                .commit(Commit.shaOnly("076c99d7bbb06b31c27a9c3164f152d5c18c5010"))
                .repo(headRepo)
                .build();

        final var diffEntries = codeLoader.loadRemoteDiff(projectRepo, base, head, 1, FileFilter.SRC_JAVA).collectList().block();
        assertNotNull(diffEntries);
        assertEquals(9, diffEntries.size());
        final var possibleRenameCount = diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.similarityIndex() > 0)
                .count();
        assertEquals(5, possibleRenameCount);
    }

    @Test
    void testLoadFilenames() {
        var projectRepo = GithubRepo.builder().id(1L).name("2017-highload-kv").isPrivate(false).description("descr").owner(USER).build();
        final var filenames = codeLoader.loadFilenames(projectRepo).collectList().block();
        assertNotNull(filenames);
        assertEquals(21, filenames.size());
        assertTrue(filenames.contains(README));
    }

    @Test
    void testLoadCommits() {
        var headRepo = GithubRepo.builder().id(1L).name("2019-highload-dht").isPrivate(false).description("descr").owner(vaddya).build();
        var sinceRef = "b5e4943c3690a54c325f7a95db20893f75b0b41b";
        var untilRef = "50bcdd747aa571e0776bed65fe474784cd73377b";
        StepVerifier.create(codeLoader.loadCommits(headRepo, sinceRef, untilRef).collectList())
                .expectNextMatches(commits -> commits.size() == 9)
                .expectComplete()
                .verify();
    }

    @Test
    void testLoadAllCommits() {
        var headRepo = GithubRepo.builder().id(1L).name("2019-highload-dht").isPrivate(false).description("descr").owner(vaddya).build();
        StepVerifier.create(codeLoader.loadAllCommits(headRepo).collectList())
                .expectNextMatches(commits -> !commits.isEmpty())
                .expectComplete()
                .verify();
    }

    @Test
    void testLoadCommitsUntilRef() {
        var headRepo = GithubRepo.builder().id(1L).name("2019-highload-dht").isPrivate(false).description("descr").owner(vaddya).build();
        StepVerifier.create(codeLoader.loadCommits(headRepo, "7a490a1e518df228c203c3690100bd2d0ab559c5").collectList())
                .expectNextMatches(commits -> commits.size() == 145)
                .expectComplete()
                .verify();
    }

    @Test
    void testLoadCommit() {
        var headRepo = GithubRepo.builder().id(1L).name("2019-highload-dht").isPrivate(false).description("descr").owner(vaddya).build();
        StepVerifier.create(codeLoader.loadCommit(headRepo, "7a490a1e518df228c203c3690100bd2d0ab559c5"))
            .expectNextMatches(commit -> commit.sha().equals("7a490a1e518df228c203c3690100bd2d0ab559c5"))
            .expectComplete()
            .verify();
    }
}
