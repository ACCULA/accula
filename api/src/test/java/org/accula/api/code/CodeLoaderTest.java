package org.accula.api.code;

import org.accula.api.code.git.Git;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Snapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    public static final GithubUser USER = new GithubUser(0L, "polis-mail-ru", "name", "ava", true);
    public static final GithubRepo REPO = new GithubRepo(0L, "2019-highload-dht", "descr", USER);
    public static final Snapshot COMMIT = Snapshot.builder()
            .sha("720cefb3f361895e9e23524c2b4025f9a949d5d2")
            .branch("branch")
            .repo(REPO)
            .build();

    CodeLoader codeLoader;

    @BeforeEach
    void beforeAll(@TempDir final Path tempDir) {
        codeLoader = new GitCodeLoader(new Git(tempDir, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())));
    }

    @Test
    void testGetSingleFile() {
        StepVerifier.create(codeLoader.loadFiles(COMMIT, README::equals))
                .expectNextMatches(readme -> readme.getContent().startsWith("# 2019-highload-dht"))
                .expectComplete()
                .verify();
    }

    @Test
    void testGetMultipleFiles() {
        IntStream.range(0, 10)
                .parallel()
                .forEach(it -> {
                    Map<String, String> files = codeLoader.loadFiles(COMMIT)
                            .collectMap(FileEntity::getName, FileEntity::getContent).block();
                    assertNotNull(files);
                    assertEquals(40, files.size());
                    assertTrue(files.containsKey(README));
                    assertTrue(files.get(README).startsWith("# 2019-highload-dht"));
                });
    }

    @Test
    void testGetMultipleFilteredFiles() {
        Pattern excludeRegex = Pattern.compile(".*Test.*");
        FileFilter filter = filename -> filename.endsWith(".java") && !excludeRegex.matcher(filename).matches();
        Map<String, String> files = codeLoader.loadFiles(COMMIT, filter)
                .collectMap(FileEntity::getName, FileEntity::getContent).block();
        assertNotNull(files);
        assertEquals(10, files.size());
        assertFalse(files.containsKey(README));
        assertFalse(files.containsKey("src/test/java/ru/mail/polis/FilesTest.java"));
        assertTrue(files.containsKey("src/main/java/ru/mail/polis/dao/DAO.java"));
        assertTrue(files.get("src/main/java/ru/mail/polis/dao/DAO.java").contains("interface DAO"));
    }

    @Test
    void testGetFileSnippetSingleLine() {
        var snippet = codeLoader.loadSnippets(COMMIT, List.of(SnippetMarker.of(README, 4, 4))).blockFirst();
        assertNotNull(snippet);
        assertEquals("""
                ## Этап 1. HTTP + storage (deadline 2019-10-05)
                """, snippet.getContent());
    }

    @Test
    void testGetFileSnippetMultiplyLines() {
        StepVerifier.create(codeLoader.loadSnippets(COMMIT, List.of(
                SnippetMarker.of(README, 4, 5),
                SnippetMarker.of("NOT_EXISTENT_FILE", 1, 1)))
                .map(FileEntity::getContent))
                .expectNextMatches(content -> content.equals("""
                        ## Этап 1. HTTP + storage (deadline 2019-10-05)
                        ### Fork
                        """))
                .expectComplete()
                .verify();
    }

    @Test
    void testGetFileSnippetWrongRange() {
        StepVerifier.create(codeLoader.loadSnippets(COMMIT, List.of(SnippetMarker.of(README, 5, 4))))
                .expectNextMatches(file -> file.getContent() == null)
                .expectComplete()
                .verify();
    }

    @Test
    void testLoadManySnippetsFromSameFile() {
        final var snippets = codeLoader
                .loadSnippets(COMMIT, List.of(
                        SnippetMarker.of(README, 1, 5),
                        SnippetMarker.of(README, 4, 10),
                        SnippetMarker.of(CLUSTER_JAVA, 38, 39),
                        SnippetMarker.of(README, 7, 15),
                        SnippetMarker.of(CLUSTER_JAVA, 51, 56)
                ))
                .collectList()
                .block();
        assertEquals(5, snippets.size());
        assertEquals("""
                # 2019-highload-dht
                Курсовой проект 2019 года [курса](https://polis.mail.ru/curriculum/program/discipline/792/) "Highload системы" в [Технополис](https://polis.mail.ru).
                                
                ## Этап 1. HTTP + storage (deadline 2019-10-05)
                ### Fork
                """, snippets.get(0).getContent());
        assertEquals("""
                ## Этап 1. HTTP + storage (deadline 2019-10-05)
                ### Fork
                [Форкните проект](https://help.github.com/articles/fork-a-repo/), склонируйте и добавьте `upstream`:
                ```
                $ git clone git@github.com:<username>/2019-highload-dht.git
                Cloning into '2019-highload-dht'...
                ...
                """, snippets.get(1).getContent());
        assertEquals("""
                    private static final int[] PORTS = {8080, 8081, 8082};
                    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
                """, snippets.get(2).getContent());
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
                """, snippets.get(3).getContent());
        assertEquals("""
                    public static void main(final String[] args) throws IOException {
                        // Fill the topology
                        final Set<String> topology = new HashSet<>(3);
                        for (final int port : PORTS) {
                            topology.add("http://localhost:" + port);
                        }
                """, snippets.get(4).getContent());
    }

    @Test
    void testDiff() {
        var headOwner = new GithubUser(1L, "vaddya", "owner", "ava", false);
        var headRepo = new GithubRepo(1L, "2019-highload-dht", "descr", headOwner);
        var head = Snapshot.builder().sha("a1c28a1b500701819cf9919246f15f3f900bb609").branch("branch").repo(headRepo).build();
        var base = Snapshot.builder().sha("d6357dccc16c7d5c001fd2a2203298c36fe96b63").branch("branch").repo(REPO).build();
        StepVerifier.create(codeLoader.loadDiff(base, head, 0, FileFilter.SRC_JAVA))
                .expectNextCount(11)
                .expectComplete()
                .verify();
    }

    @Test
    void testRemoteDiff() {
        final var projectRepo = new GithubRepo(1L, "2017-highload-kv", "descr", USER);
        final var base = Snapshot
                .builder()
                .sha("fe675f17ad4aab9a8c853b5f3b07b0bc64f06907")
                .repo(new GithubRepo(0L, "2017-highload-kv", "", new GithubUser(0L, "lamtev", null, "", false)))
                .build();
        final var head = Snapshot
                .builder()
                .sha("076c99d7bbb06b31c27a9c3164f152d5c18c5010")
                .repo(new GithubRepo(0L, "2017-highload-kv", "", new GithubUser(0L, "vaddya", null, "", false)))
                .build();

        final var diffEntries = codeLoader.loadRemoteDiff(projectRepo, base, head, 1, FileFilter.SRC_JAVA).collectList().block();
        assertEquals(9, diffEntries.size());
        final var possibleRenameCount = diffEntries
                .stream()
                .filter(diffEntry -> diffEntry.getSimilarityIndex() > 0)
                .count();
        assertEquals(5, possibleRenameCount);
    }
}
