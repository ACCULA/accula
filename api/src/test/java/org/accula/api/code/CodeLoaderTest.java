package org.accula.api.code;

import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FIXME: replace block with StepVerifier
 *
 * @author Vadim Dyachkov
 */
class CodeLoaderTest {
    public static final String README = "README.md";
    public static final GithubUser USER = new GithubUser(0L, "polis-mail-ru", "name", "ava", true);
    public static final GithubRepo REPO = new GithubRepo(0L, "2019-highload-dht", "descr", USER);
    public static final CommitSnapshot COMMIT = CommitSnapshot.builder()
            .sha("720cefb3f361895e9e23524c2b4025f9a949d5d2")
            .branch("branch")
            .repo(REPO)
            .build();

    private static CodeLoader codeLoader;

    @BeforeAll
    static void beforeAll(@TempDir final File tempDir) {
        codeLoader = new JGitCodeLoader(tempDir);
    }

    @Test
    void testGetSingleFile() {
        StepVerifier.create(codeLoader.getFiles(COMMIT, README::equals))
                .expectNextMatches(readme -> readme.getContent().startsWith("# 2019-highload-dht"))
                .expectComplete()
                .verify();
    }

    @Test
    void testGetMultipleFiles() {
        Map<String, String> files = codeLoader.getFiles(COMMIT)
                .collectMap(FileEntity::getName, FileEntity::getContent).block();
        assertNotNull(files);
        assertEquals(40, files.size());
        assertTrue(files.containsKey(README));
        assertTrue(files.get(README).startsWith("# 2019-highload-dht"));
    }

    @Test
    void testGetMultipleFilteredFiles() {
        Pattern excludeRegex = Pattern.compile(".*Test.*");
        FileFilter filter = fileName -> fileName.endsWith(".java") && !excludeRegex.matcher(fileName).matches();
        Map<String, String> files = codeLoader.getFiles(COMMIT, filter)
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
        FileEntity snippet = codeLoader.getFileSnippet(COMMIT, README, 4, 4).block();
        assertNotNull(snippet);
        assertEquals("## Этап 1. HTTP + storage (deadline 2019-10-05)", snippet.getContent());
    }

    @Test
    void testGetFileSnippetMultiplyLines() {
        StepVerifier.create(codeLoader.getFileSnippet(COMMIT, README, 4, 5)
                .map(FileEntity::getContent))
                .expectNextMatches(content -> content.equals("## Этап 1. HTTP + storage (deadline 2019-10-05)\n### Fork"))
                .expectComplete()
                .verify();
    }

    @Test
    void testGetFileSnippetWrongRange() {
        assertThrows(Exception.class, () -> codeLoader.getFileSnippet(COMMIT, README, 5, 4).block());
    }

    @Test
    void testDiff() {
        var headOwner = new GithubUser(1L, "vaddya", "owner", "ava", false);
        var headRepo = new GithubRepo(1L, "2019-highload-dht", "descr", headOwner);
        var head = CommitSnapshot.builder().sha("a1c28a1b500701819cf9919246f15f3f900bb609").branch("branch").repo(headRepo).build();
        var base = CommitSnapshot.builder().sha("d6357dccc16c7d5c001fd2a2203298c36fe96b63").branch("branch").repo(REPO).build();
        List<Tuple2<FileEntity, FileEntity>> diff = codeLoader.getDiff(base, head).collectList().block();
        assertNotNull(diff);
        assertEquals(18, diff.size());
    }
}
