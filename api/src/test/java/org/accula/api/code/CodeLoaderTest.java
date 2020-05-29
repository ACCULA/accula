package org.accula.api.code;

import org.accula.api.db.model.Commit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.util.function.Tuple2;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FIXME: replace block with StepVerifier
 *
 * @author Vadim Dyachkov
 */
class CodeLoaderTest {
    public static final String OWNER = "polis-mail-ru";
    public static final String REPO = "2019-highload-dht";
    public static final String SHA = "720cefb3f361895e9e23524c2b4025f9a949d5d2";
    public static final String README = "README.md";
    public static final Commit COMMIT = new Commit(0L, OWNER, REPO, SHA);

    private static CodeLoader codeLoader;

    @BeforeAll
    static void beforeAll(@TempDir final File tempDir) {
        RepositoryProvider repositoryProvider = new RepositoryManager(tempDir);
        codeLoader = new CodeLoaderImpl(repositoryProvider);
    }

    @Test
    void testGetSingleFile() {
        FileEntity readme = codeLoader.getFile(COMMIT, README).block();
        assertNotNull(readme);
        assertTrue(readme.getContent().startsWith("# 2019-highload-dht"));
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
        FileEntity snippet = codeLoader.getFileSnippet(COMMIT, README, 4, 5).block();
        assertNotNull(snippet);
        assertEquals("## Этап 1. HTTP + storage (deadline 2019-10-05)\n### Fork", snippet.getContent());
    }

    @Test
    void testGetFileSnippetWrongRange() {
        assertThrows(Exception.class, () -> {
            codeLoader.getFileSnippet(COMMIT, README, 5, 4).block();
        });
    }

    @Test
    void testDiff() {
        Commit base = new Commit(0L, OWNER, REPO, "d6357dccc16c7d5c001fd2a2203298c36fe96b63");
        Commit head = new Commit(1L, "vaddya", REPO, "a1c28a1b500701819cf9919246f15f3f900bb609");
        List<Tuple2<FileEntity, FileEntity>> diff = codeLoader.getDiff(base, head).collectList().block();
        assertNotNull(diff);
        assertEquals(18, diff.size());
    }
}