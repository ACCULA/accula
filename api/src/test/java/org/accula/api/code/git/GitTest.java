package org.accula.api.code.git;

import lombok.SneakyThrows;
import org.accula.api.code.lines.LineRange;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.accula.api.code.lines.LineRange.of;
import static org.accula.api.code.lines.LineSet.inRange;
import static org.accula.api.code.lines.LineSet.of;
import static org.accula.api.util.TestData.accula69f5528Git;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Anton Lamtev
 */
final class GitTest {
    static final String REPO_URL = "https://github.com/ACCULA/accula.git";
    static final String REPO_DIR = "accula";
    static final String BASE_REF = "7a019e571e2716f7f133e1a63a49f300e03aea00";
    static final String HEAD_REF = "69f552851f0f6093816c3064b6e00438e0ff3b19";
    static final String REMOTE_URL = "https://github.com/lamtev/poker.git";
    static final String REMOTE_NAME = "newRemote";
    Git git;

    @BeforeEach
    void setUp(@TempDir final Path dir) {
        git = new Git(dir, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3));
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
            final Git.Repo repo = testRepo();

            final var diffEntries1 = repo.diff(BASE_REF, HEAD_REF, 0).get();
            final var diffEntries2 = repo.diff(BASE_REF, HEAD_REF, 100).get();
            assertEquals(45, diffEntries1.size());
            assertEquals(diffEntries1, diffEntries2);

            final var diffEntries3 = repo.diff(BASE_REF, HEAD_REF, 3).get();
            final var diffEntries4 = repo.diff(BASE_REF, HEAD_REF, 7).get();
            assertEquals(41, diffEntries3.size());
            assertEquals(44, diffEntries4.size());
        });
    }

    @Test
    void testCatFiles() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            final var filesContent = repo.diff(BASE_REF, HEAD_REF, 0)
                    .thenCompose(diffEntries -> repo
                            .catFiles(diffEntries
                                    .stream()
                                    .flatMap(GitDiffEntry::objectIds)
                                    .toList()))
                    .get();
            assertTrue(filesContent.values().stream().anyMatch("""
                    @NonNullApi
                    package org.accula.api.auth.jwt.crypto;
                                        
                    import org.springframework.lang.NonNullApi;
                    """::equals));
        });
    }

    @Test
    void testCatFilesEmptyInput() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            assertTrue(repo.catFiles(Collections.emptyList()).get().isEmpty());
        });
    }

    @Test
    void testCatSnippets() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            final var snippet = new Snippet(
                    GitFile.of("3c19d0d", "api/src/main/java/org/accula/api/auth/jwt/JwtRefreshFilter.java"),
                    LineRange.of(10, 38)
            );

            final var snippetsContent = repo.catFiles(List.of(snippet)).get();
            assertNotNull(snippetsContent);
            assertEquals(1, snippetsContent.size());

            assertEquals("""
                    import org.springframework.web.server.WebFilterChain;
                    import reactor.core.publisher.Mono;
                                        
                    import java.time.Duration;
                                        
                    /**
                     * Web filter that refreshes an access token using refresh token provided in cookies.
                     * <p>New refresh token replaces the previous one in DB
                     * ({@link RefreshTokenRepository}) as well as in client cookies.
                     * <p>Response is constructed using {@link JwtAccessTokenResponseProducer}.
                     *
                     * @author Anton Lamtev
                     */
                    @RequiredArgsConstructor
                    public final class JwtRefreshFilter implements WebFilter {
                        private final ServerWebExchangeMatcher endpointMatcher;
                        private final JwtAccessTokenResponseProducer responseProducer;
                        private final Jwt jwt;
                        private final Duration refreshExpiresIn;
                        private final RefreshTokenRepository refreshTokens;
                                        
                        @Override
                        public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
                            return endpointMatcher
                                    .matches(exchange)
                                    .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                                    .flatMap(match -> doRefreshToken(exchange))
                                    .switchIfEmpty(chain.filter(exchange).then(Mono.empty()));
                        }
                    """, snippetsContent.values().iterator().next());
        });
    }

    @Test
    void testShow() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            final var files = repo.show(HEAD_REF).get();
            assertEquals(41, files.size());
        });
    }

    @Test
    void testShowMany() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            final var shas = Set.of(
                    "183a85a2342f521e3ddc7ecab87d3dcbc9256dd4",
                    "ad79a04a8434eadb851053378ad5824f249c8aae",
                    "2719f27e3fa45988fdcc791fc08e298f9f4b05a5");
            final var entries = repo.show(shas).get();
            assertEquals(6, entries.size());
            entries.forEach((entry, sha) -> assertTrue(shas.contains(sha)));
        });
    }

    @Test
    void testLsTree() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            final var files = repo.lsTree(HEAD_REF).get();
            assertEquals(69, files.size());
        });
    }

    @Test
    void testRemote() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            final var remotes = repo.remote().get();
            assertTrue(remotes.contains("origin"));
        });
    }

    @Test
    void testSimultaneousRemoteAdding() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                    .parallel()
                    .forEach(it -> assertDoesNotThrow(() ->
                            assertNotNull(repo.remoteAdd(REMOTE_URL, REMOTE_NAME))));
        });
    }

    @Test
    void testSimultaneousRemoteUpdate() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();
            assertNotNull(repo.remoteAdd(REMOTE_URL, REMOTE_NAME).get());

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                    .parallel()
                    .forEach(it -> assertDoesNotThrow(() ->
                            assertNotNull(repo.remoteUpdate(REMOTE_NAME))));
        });
    }

    @Test
    void testLog() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                    .parallel()
                    .forEach(it ->
                            assertDoesNotThrow(() -> {
                                final var commits = repo.log(BASE_REF, HEAD_REF).get();
                                assertEquals(1, commits.size());
                                assertEquals(accula69f5528Git, commits.get(0));
                            }));
        });
    }

    @Test
    void testLogUntilRef() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                    .parallel()
                    .forEach(it ->
                            assertDoesNotThrow(() -> {
                                final var commits = repo.log(HEAD_REF).get();
                                assertEquals(10, commits.size());
                            }));
        });
    }

    @Test
    void testLogSingle() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                .parallel()
                .forEach(it ->
                    assertDoesNotThrow(() -> {
                        final var commits = repo.logSingle(HEAD_REF).get();
                        assertEquals(HEAD_REF, commits.sha());
                    }));
        });
    }

    @Test
    void testLogSingleFail() {
        assertThrows(ExecutionException.class, () -> {
            final Git.Repo repo = testRepo();
            final var commits = repo.logSingle("NOT_EXISTENT_REF").get();
            assertEquals(HEAD_REF, commits.sha());
        });
    }

    @Test
    void testRevListAllPretty() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                    .parallel()
                    .forEach(it ->
                            assertDoesNotThrow(() -> {
                                final var commits = repo.revListAllRemotesPretty().get();
                                assertFalse(commits.isEmpty());
                            }));
        });
    }

    @Test
    @SneakyThrows
    void testParseShowEntryRareCases() {
        var parseShowEntry = git.getClass().getDeclaredMethod("parseShowEntry", String.class);
        if (!parseShowEntry.canAccess(null)) {
            parseShowEntry.setAccessible(true);
        }

        var f1 = (GitFile) parseShowEntry.invoke(null, "::100644 100644 100644 d524b59 70f0dfc eb61c91 MM\tbuild.gradle.kts");
        assertEquals("eb61c91", f1.id());
        assertEquals("build.gradle.kts", f1.name());

        var f2 = (GitFile) parseShowEntry.invoke(null, "::100644 100644 100644 d524b59 70f0dfc eb61c91 MR build.gradle.kts build.gradle");
        assertEquals("eb61c91", f2.id());
        assertEquals("build.gradle", f2.name());
    }

    @Test
    void testFileChanges() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            final var expectedChanges = List.of(
                    FileDiff.of(GitFile.of("9b1113b", ".codeclimate.yml"), of(of(10), of(33, 35))),
                    FileDiff.of(GitFile.of("e703c8e", "api/build.gradle.kts"), inRange(of(35))),
                    FileDiff.of(GitFile.of("b84f9ad", "api/src/main/java/org/accula/api/auth/jwt/JwtAccessTokenResponseProducer.java"), of(of(25, 26), of(50, 51), of(53), of(58), of(68, 82))),
                    FileDiff.of(GitFile.of("f3c4044", "api/src/main/java/org/accula/api/auth/jwt/refresh/JwtRefreshFilter.java"), of(of(1), of(4, 5), of(19, 22), of(33), of(36, 38), of(46), of(55, 56), of(65), of(75, 76), of(78, 84))),
                    FileDiff.of(GitFile.of("a77479f", "api/src/main/java/org/accula/api/auth/jwt/refresh/RefreshTokenException.java"), inRange(1, 23)),
                    FileDiff.of(GitFile.of("827e34c", "api/src/main/java/org/accula/api/auth/jwt/refresh/package-info.java"), inRange(1, 4)),
                    FileDiff.of(GitFile.of("0743994", "api/src/main/java/org/accula/api/auth/oauth2/OAuth2LoginSuccessHandler.java"), inRange(31, 32)),
                    FileDiff.of(GitFile.of("d66e521", "api/src/main/java/org/accula/api/config/WebSecurityConfig.java"), of(of(9), of(52, 53), of(125), of(178, 179))),
                    FileDiff.of(GitFile.of("c505a06", "api/src/main/java/org/accula/api/db/RefreshTokenRepository.java"), of(of(17), of(19, 21), of(26))),
                    FileDiff.of(GitFile.of("5910d7c", "pmd.xml"), inRange(59, 69))
            );

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                    .parallel()
                    .forEach(it ->
                            assertDoesNotThrow(() -> {
                                final var changes = repo.fileChanges("f653ff42259d82f782f1284bd35bae5bec02047f").get();
                                assertEquals(expectedChanges, changes);
                            }));
        });
    }

    @Test
    void testFileChangesMany() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                    .parallel()
                    .forEach(it ->
                            assertDoesNotThrow(() -> {
                                final var changes = repo.fileChanges(List.of(
                                        "3d9df071503e977674b7300900156ab330f553df",
                                        "ad79a04a8434eadb851053378ad5824f249c8aae",
                                        "d7c7ec06737b82049140f6baf30ac70d9e6d5ed5",
                                        "69f552851f0f6093816c3064b6e00438e0ff3b19" // 45 changes, but 4 of them are deletions that we don't track
                                )).get();
                                assertEquals(72 + 2 + 1 + 41, changes.size());
                            }));
        });
    }

    @Test
    void testRevListAllRemotes() {
        assertDoesNotThrow(() -> {
            final Git.Repo repo = testRepo();

            IntStream.range(0, Runtime.getRuntime().availableProcessors() * 5)
                .parallel()
                .forEach(it ->
                    assertDoesNotThrow(() -> {
                        final var commits = repo.revListAllRemotes().get();
                        assertFalse(commits.isEmpty());
                        assertTrue(commits.contains("f4cf7c5bec1e12355d5232e88c9fd0c423341a59"));
                    }));
        });
    }

    @NotNull
    private Git.Repo testRepo() throws InterruptedException, ExecutionException {
        final var repo = git.clone(REPO_URL, REPO_DIR).get();
        assertNotNull(repo);
        return repo;
    }
}
