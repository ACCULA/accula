package org.accula.api.routers;

import org.accula.api.code.CodeLoader;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.SnapshotRepo;
import org.accula.api.github.model.GithubApiHookPayload;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiSnapshot;
import org.accula.api.github.model.GithubApiUser;
import org.accula.api.handler.GithubWebhookHandler;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.accula.api.handler.GithubWebhookHandler.GITHUB_EVENT;
import static org.accula.api.handler.GithubWebhookHandler.GITHUB_EVENT_PING;
import static org.accula.api.handler.GithubWebhookHandler.GITHUB_EVENT_PULL;
import static org.accula.api.routers.ProjectsRouterTest.GH_OWNER;
import static org.accula.api.routers.ProjectsRouterTest.GH_OWNER_HIGHLOAD;
import static org.accula.api.routers.ProjectsRouterTest.GH_REPO_HIGHLOAD1;
import static org.accula.api.routers.ProjectsRouterTest.GH_REPO_HIGHLOAD2;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@ContextConfiguration(classes = {
    GithubWebhookRouter.class,
    GithubWebhookHandler.class,
    ProjectService.class,
})
class GithubWebhookRouterTest {
    static final GithubApiSnapshot base = GithubApiSnapshot.builder()
        .label("master")
        .ref("master")
        .user(GH_REPO_HIGHLOAD1.owner())
        .repo(GH_REPO_HIGHLOAD1)
        .sha("sha1")
        .build();
    static final GithubApiSnapshot head = GithubApiSnapshot.builder()
        .label("master")
        .ref("master")
        .user(GH_REPO_HIGHLOAD2.owner())
        .repo(GH_REPO_HIGHLOAD2)
        .sha("sha2")
        .build();
    static final GithubApiPull pull = GithubApiPull.builder()
        .id(1L)
        .htmlUrl("url")
        .head(head)
        .base(base)
        .user(GH_OWNER)
        .number(2)
        .title("title")
        .state(GithubApiPull.State.OPEN)
        .createdAt(Instant.EPOCH)
        .updatedAt(Instant.now())
        .assignee(GH_OWNER)
        .assignees(new GithubApiUser[]{GH_REPO_HIGHLOAD2.owner(), GH_OWNER_HIGHLOAD})
        .build();
    @Autowired
    RouterFunction<ServerResponse> webhookRoute;
    WebTestClient client;
    @MockBean
    ProjectRepo projectRepo;
    @MockBean
    GithubUserRepo githubUserRepo;
    @MockBean
    GithubRepoRepo githubRepoRepo;
    @MockBean
    SnapshotRepo snapshotRepo;
    @MockBean
    PullRepo pullRepo;
    @MockBean
    CodeLoader codeLoader;
    @Autowired
    ProjectService projectService;
    @MockBean
    CloneDetectionService cloneDetectionService;

    @BeforeEach
    public void setUp() {
        client = WebTestClient
            .bindToRouterFunction(webhookRoute)
            .build();
    }

    @Test
    void testPing() {
        client.post().uri("/api/webhook")
            .header(GITHUB_EVENT, GITHUB_EVENT_PING)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testAssigned() {
        when(githubUserRepo.upsert(anyCollection()))
            .thenReturn(Flux.empty());
        when(githubRepoRepo.upsert(anyCollection()))
            .thenReturn(Flux.empty());
        when(pullRepo.upsert(any(Pull.class)))
            .thenReturn(Mono.just(GithubApiToModelConverter.convert(pull)));

        client.post().uri("/api/webhook")
            .header(GITHUB_EVENT, GITHUB_EVENT_PULL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(GithubApiHookPayload.builder()
                .action(GithubApiHookPayload.Action.ASSIGNED)
                .repo(GH_REPO_HIGHLOAD1)
                .pull(pull)
                .build())
            .exchange()
            .expectStatus().isOk();
    }
}
