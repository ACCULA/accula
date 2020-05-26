package org.accula.api.code;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CodeClientTest {
    public static final String OWNER = "polis-mail-ru";
    public static final String REPO = "2019-highload-dht";
    public static final String SHA = "720cefb3f361895e9e23524c2b4025f9a949d5d2";
    public static final String README = "README.md";

    private CodeClient codeClient;

    @BeforeEach
    void setUp(@TempDir final File tempDir) {
        RepositoryManager repositoryManager = new RepositoryManagerImpl(tempDir);
        this.codeClient = new CodeClientImpl(repositoryManager);
    }

    @Test
    void testGetSingleFile() {
        String readme = codeClient.getFile(OWNER, REPO, SHA, README).block();
        assertNotNull(readme);
        assertTrue(readme.startsWith("# 2019-highload-dht"));
    }

    @Test
    void testGetMultipleFiles() {
        Map<String, String> files = codeClient.getFiles(OWNER, REPO, SHA)
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
        Map<String, String> files = codeClient.getFiles(OWNER, REPO, SHA, filter)
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
        String snippet = codeClient.getFileSnippet(OWNER, REPO, SHA, README, 4, 4).block();
        assertNotNull(snippet);
        assertEquals("## Этап 1. HTTP + storage (deadline 2019-10-05)", snippet);
    }

    @Test
    void testGetFileSnippetMultiplyLines() {
        String snippet = codeClient.getFileSnippet(OWNER, REPO, SHA, README, 4, 5).block();
        assertNotNull(snippet);
        assertEquals("## Этап 1. HTTP + storage (deadline 2019-10-05)\n### Fork", snippet);
    }

    @Test
    void testGetFileSnippetWrongRange() {
        assertThrows(Exception.class, () -> {
            codeClient.getFileSnippet(OWNER, REPO, SHA, README, 5, 4).block();
        });
    }
}
