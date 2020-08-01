package org.accula.api.code.git;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Anton Lamtev
 */
final class GitTest {
    private static final String REPO_URL = "https://github.com/ACCULA/accula.git";
    private static final String REPO_DIR = "accula";
    private static final String BASE_REF = "7a019e571e2716f7f133e1a63a49f300e03aea00";
    private static final String HEAD_REF = "69f552851f0f6093816c3064b6e00438e0ff3b19";
    public static final String REMOTE_URL = "https://github.com/lamtev/poker.git";
    public static final String REMOTE_NAME = "newRemote";
    private static Git git;

    @BeforeAll
    static void setUp(@TempDir final Path dir) {
        git = new Git(dir, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    @Test
    void testSimultaneousCloning() {
        IntStream
                .range(0, Runtime.getRuntime().availableProcessors() * 10)
                .parallel()
                .forEach(it -> assertDoesNotThrow(() -> {
                    try {
                        assertNotNull(git.clone(REPO_URL, REPO_DIR).get());
                    } catch (InterruptedException | ExecutionException e) {
                        fail(e);
                    }
                }));
    }

    @Test
    void testRepoAvailableAfterCloning() {
        assertDoesNotThrow(() -> {
            git.clone(REPO_URL, REPO_DIR).get();

            assertNotNull(git.repo(Path.of(REPO_DIR)).get());
        });
    }

    @Test
    void testSimultaneousFetch() {
        assertDoesNotThrow(() -> {
            final var future = git.clone(REPO_URL, REPO_DIR)
                    .thenAccept(repo -> IntStream
                            .range(0, Runtime.getRuntime().availableProcessors())
                            .parallel()
                            .forEach(it -> assertDoesNotThrow(() -> assertNotNull(repo.fetch().get()))));
            future.get();
        });
    }

    @Test
    void testDiff() {
        assertDoesNotThrow(() -> {
            final var repo = git.clone(REPO_URL, REPO_DIR).get();
            assertNotNull(repo);

            final var diffEntries = repo.diff(BASE_REF, HEAD_REF, 0).get();
            assertEquals(45, diffEntries.size());
        });
    }

    @Test
    void testCatFiles() {
        assertDoesNotThrow(() -> {
            final var repo = git.clone(REPO_URL, REPO_DIR).get();
            assertNotNull(repo);

            final var filesContent = repo.diff(BASE_REF, HEAD_REF, 0)
                    .thenCompose(diffEntries -> repo
                            .catFiles(diffEntries
                                    .stream()
                                    .flatMap(DiffEntry::objectIds)
                                    .collect(toList())))
                    .get();
            assertTrue(filesContent.values().stream().anyMatch("""
                    @NonNullApi
                    package org.accula.api.auth.jwt.crypto;
                                        
                    import org.springframework.lang.NonNullApi;
                    """::equals));
        });
    }

    @Test
    void testShow() {
        assertDoesNotThrow(() -> {
            final var repo = git.clone(REPO_URL, REPO_DIR).get();
            assertNotNull(repo);

            final var files = repo.show(HEAD_REF).get();
            assertEquals(41, files.size());
        });
    }

    @Test
    void testLsTree() {
        assertDoesNotThrow(() -> {
            final var repo = git.clone(REPO_URL, REPO_DIR).get();
            assertNotNull(repo);

            final var files = repo.lsTree(HEAD_REF).get();
            assertEquals(69, files.size());
        });
    }

    @Test
    void testRemote() {
        assertDoesNotThrow(() -> {
            final var repo = git.clone(REPO_URL, REPO_DIR).get();
            assertNotNull(repo);

            final var remotes = repo.remote().get();
            assertTrue(remotes.contains("origin"));
        });
    }

    @Test
    void testSimultaneousRemoteAdding() {
        assertDoesNotThrow(() -> {
            final var repo = git.clone(REPO_URL, REPO_DIR).get();
            assertNotNull(repo);

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                    .parallel()
                    .forEach(it -> assertDoesNotThrow(() ->
                            assertNotNull(repo.remoteAdd(REMOTE_URL, REMOTE_NAME))));
        });
    }

    @Test
    void testSimultaneousRemoteUpdate() {
        assertDoesNotThrow(() -> {
            final var repo = git.clone(REPO_URL, REPO_DIR).get();
            assertNotNull(repo);
            assertNotNull(repo.remoteAdd(REMOTE_URL, REMOTE_NAME).get());

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                    .parallel()
                    .forEach(it -> assertDoesNotThrow(() ->
                            assertNotNull(repo.remoteUpdate(REMOTE_NAME))));
        });
    }
}
