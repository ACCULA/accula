package org.accula.api.routers;

import lombok.SneakyThrows;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.code.SnippetMarker;
import org.accula.api.code.lines.LineRange;
import org.accula.api.code.lines.LineSet;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.handler.ClonesHandler;
import org.accula.api.handler.dto.CloneDto;
import org.accula.api.service.CloneDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@ContextConfiguration(classes = {
    ClonesRouter.class,
    ClonesHandler.class,
})
class ClonesRouterTest {
    @Autowired
    RouterFunction<ServerResponse> clonesRoute;
    WebTestClient client;
    @MockBean
    CloneRepo cloneRepo;
    @MockBean
    PullRepo pullRepo;
    @MockBean
    CurrentUserRepo currentUserRepo;
    @MockBean
    ProjectRepo projectRepo;
    @MockBean
    CloneDetectionService cloneDetectionService;
    @MockBean
    CodeLoader codeLoader;

    static final GithubUser usr1 = GithubUser.builder()
        .id(1L)
        .login("Bad_guy")
        .name("Bad guy")
        .avatar("ava")
        .isOrganization(false)
        .build();
    static final GithubRepo repo1 = GithubRepo.builder()
        .id(1L)
        .name("2020-db-lsm")
        .isPrivate(false)
        .description("No Sql course")
        .owner(usr1)
        .build();
    static final Snapshot snap1 = Snapshot.builder()
        .commit(Commit.shaOnly("sha1"))
        .branch("main")
        .repo(repo1)
        .pullInfo(Snapshot.PullInfo.of(1L, 25))
        .build();
    static final Clone.Snippet target = Clone.Snippet.builder()
        .id(1L)
        .snapshot(snap1)
        .file("path/to/file/File.java")
        .fromLine(130)
        .toLine(155)
        .build();
    static final GithubUser usr2 = GithubUser.builder()
        .id(1L)
        .login("Good_guy")
        .name("Good guy")
        .avatar("ava")
        .isOrganization(false)
        .build();
    static final GithubRepo repo2 = GithubRepo.builder()
        .id(1L)
        .name("2020-db-lsm")
        .isPrivate(false)
        .description("No Sql course")
        .owner(usr2)
        .build();
    static final Snapshot snap2 = Snapshot.builder()
        .commit(Commit.shaOnly("sha2"))
        .branch("master")
        .repo(repo2)
        .pullInfo(Snapshot.PullInfo.of(2L, 10))
        .build();
    static final Clone.Snippet source = Clone.Snippet.builder()
        .id(2L)
        .snapshot(snap2)
        .file("path1/to1/file1/File1.java")
        .fromLine(139)
        .toLine(164)
        .build();
    static final Pull pull1 = Pull.builder()
        .id(1L)
        .number(25)
        .head(snap1)
        .base(snap2)
        .build();
    static final Pull pull2 = Pull.builder()
        .id(2L)
        .number(10)
        .head(snap2)
        .base(snap1)
        .build();
    static final List<Clone> clones = List.of(
        Clone.builder()
            .id(1L)
            .target(target)
            .source(source)
            .build()
    );
    static final List<FileEntity<Snapshot>> snippets = List.of(
        new FileEntity<>(snap1, target.file(), "", LineSet.inRange(target.fromLine(), target.toLine())),
        new FileEntity<>(snap2, source.file(), "", LineSet.inRange(source.fromLine(), source.toLine()))
    );

    @BeforeEach
    void setUp() {
        client = WebTestClient
            .bindToRouterFunction(clonesRoute)
            .build();
    }

    @SneakyThrows
    @Test
    void testGetClonesByPullNumber() {
        when(cloneRepo.findByPullNumber(anyLong(), anyInt()))
            .thenReturn(Flux.fromIterable(clones));
        when(codeLoader.loadSnippets(snap1, List.of(SnippetMarker.of(target.file(), LineRange.of(target.fromLine(), target.toLine())))))
            .thenReturn(Flux.just(snippets.get(0)));
        when(codeLoader.loadSnippets(snap2, List.of(SnippetMarker.of(source.file(), LineRange.of(source.fromLine(), source.toLine())))))
            .thenReturn(Flux.just(snippets.get(1)));
        when(pullRepo.findById(anyCollection()))
            .thenReturn(Flux.just(pull1, pull2));

        client.get().uri("/api/projects/{projectId}/pulls/{pullNumber}/clones", 1L, 1)
            .exchange()
            .expectStatus().isOk()
            .expectBody(CloneDto[].class).value(clones -> assertEquals(1, clones.length));
    }
}