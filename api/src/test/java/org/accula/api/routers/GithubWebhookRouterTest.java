package org.accula.api.routers;

import org.accula.api.code.CodeLoader;
import org.accula.api.config.WebConfig;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.CurrentUserRepo;
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
import org.accula.api.handler.dto.ApiError;
import org.accula.api.handler.signature.Signatures;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.service.ProjectService;
import org.accula.api.util.SerializationHelper;
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
import static org.accula.api.handler.GithubWebhookHandler.GITHUB_SIGNATURE;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.INVALID_SIGNATURE;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.MISSING_EVENT;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.MISSING_SIGNATURE;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.SIGNATURE_VERIFICATION_FAILED;
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
    WebConfig.class,
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
    static final String signatureSecret = "accula";
    static final String pingBody = "some ping body";
    static final String pingSignature = "sha256=" + Signatures.signHexHmacSha256(pingBody, signatureSecret);
    static final GithubApiHookPayload assignedPayload = GithubApiHookPayload.builder()
        .action(GithubApiHookPayload.Action.ASSIGNED)
        .repo(GH_REPO_HIGHLOAD1)
        .pull(pull)
        .build();
    static final String assignedPayloadSignature = "sha256=" + Signatures.signHexHmacSha256(SerializationHelper.json(assignedPayload), signatureSecret);
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
    @MockBean
    CurrentUserRepo currentUserRepo;

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
            .header(GITHUB_SIGNATURE, pingSignature)
            .bodyValue(pingBody)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testMissingEvent() {
        client.post().uri("/api/webhook")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(MISSING_EVENT));
    }

    @Test
    void testMissingSignature() {
        client.post().uri("/api/webhook")
            .header(GITHUB_EVENT, GITHUB_EVENT_PING)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(MISSING_SIGNATURE));
    }

    @Test
    void testInvalidSignature() {
        client.post().uri("/api/webhook")
            .header(GITHUB_EVENT, GITHUB_EVENT_PING)
            .header(GITHUB_SIGNATURE, "signature")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(INVALID_SIGNATURE));
    }

    @Test
    void testSignatureVerificationFailed() {
        client.post().uri("/api/webhook")
            .header(GITHUB_EVENT, GITHUB_EVENT_PING)
            .header(GITHUB_SIGNATURE, pingSignature)
            .bodyValue(pingBody + "salt")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(SIGNATURE_VERIFICATION_FAILED));
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
            .header(GITHUB_SIGNATURE, assignedPayloadSignature)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(SerializationHelper.json(assignedPayload))
            .exchange()
            .expectStatus().isOk();
    }
}
