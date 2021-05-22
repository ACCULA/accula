package org.accula.api.routers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.accula.api.code.CodeLoader;
import org.accula.api.config.WebConfig;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.PullSnapshots;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.SnapshotRepo;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiPullHookPayload;
import org.accula.api.github.model.GithubApiPushHookPayload;
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
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.accula.api.github.model.GithubApiPullHookPayload.Action.CONVERTED_TO_DRAFT;
import static org.accula.api.github.model.GithubApiPullHookPayload.Action.SYNCHRONIZE;
import static org.accula.api.github.model.GithubApiPullHookPayload.Action.UNASSIGNED;
import static org.accula.api.handler.GithubWebhookHandler.GITHUB_EVENT;
import static org.accula.api.handler.GithubWebhookHandler.GITHUB_EVENT_PING;
import static org.accula.api.handler.GithubWebhookHandler.GITHUB_EVENT_PULL;
import static org.accula.api.handler.GithubWebhookHandler.GITHUB_EVENT_PUSH;
import static org.accula.api.handler.GithubWebhookHandler.GITHUB_SIGNATURE;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.INVALID_SIGNATURE;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.MISSING_EVENT;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.MISSING_SIGNATURE;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.NOT_SUPPORTED_EVENT;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.SIGNATURE_VERIFICATION_FAILED;
import static org.accula.api.handler.GithubWebhookHandler.WebhookError.UNABLE_TO_DESERIALIZE_PAYLOAD;
import static org.accula.api.routers.ProjectsRouterTest.GH_OWNER;
import static org.accula.api.routers.ProjectsRouterTest.GH_OWNER_HIGHLOAD;
import static org.accula.api.routers.ProjectsRouterTest.GH_REPO_HIGHLOAD1;
import static org.accula.api.routers.ProjectsRouterTest.GH_REPO_HIGHLOAD2;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@ContextConfiguration(classes = {
    WebConfig.class,
    GithubWebhookRouter.class,
    GithubWebhookHandler.class,
})
public class GithubWebhookRouterTest {
    static final String API_WEBHOOK_ENDPOINT = "/api/webhook";
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
    public static final GithubApiPullHookPayload pullOpenedPayload = GithubApiPullHookPayload.builder()
        .action(GithubApiPullHookPayload.Action.OPENED)
        .repo(GH_REPO_HIGHLOAD1)
        .pull(pull)
        .build();
    public static final GithubApiPullHookPayload pullSynchronizePayload = pullOpenedPayload.withAction(SYNCHRONIZE);
    public static final GithubApiPullHookPayload assignedPayload = GithubApiPullHookPayload.builder()
        .action(GithubApiPullHookPayload.Action.ASSIGNED)
        .repo(GH_REPO_HIGHLOAD1)
        .pull(pull)
        .build();
    static final String signatureSecret = "accula";
    static final String pingBody = "some ping body";
    static final String pingSignature = "sha256=" + Signatures.signHexHmacSha256(pingBody, signatureSecret);
    static final String assignedPayloadSignature = "sha256=" + Signatures.signHexHmacSha256(SerializationHelper.json(assignedPayload), signatureSecret);
    static final GithubApiPullHookPayload unassignedPayload = assignedPayload.withAction(UNASSIGNED).withPull(null);
    static final String unassignedPayloadSignature = "sha256=" + Signatures.signHexHmacSha256(SerializationHelper.json(unassignedPayload), signatureSecret);
    static final GithubApiPushHookPayload pushPayload = GithubApiPushHookPayload.builder()
        .ref("master")
        .before("sha1")
        .after("sha2")
        .repo(GH_REPO_HIGHLOAD2)
        .build();
    public static final GithubApiPullHookPayload pullConvertedToDraftPayload = assignedPayload.withAction(CONVERTED_TO_DRAFT);
    static final String pushPayloadSignature = "sha256=" + Signatures.signHexHmacSha256(SerializationHelper.json(pushPayload), signatureSecret);
    static final String openedPayloadSignature = "sha256=" + Signatures.signHexHmacSha256(SerializationHelper.json(pullOpenedPayload), signatureSecret);
    static final String synchronizePayloadSignature = "sha256=" + Signatures.signHexHmacSha256(SerializationHelper.json(pullSynchronizePayload), signatureSecret);
    static final String convertedToDraftPayloadSignature = "sha256=" + Signatures.signHexHmacSha256(SerializationHelper.json(pullConvertedToDraftPayload), signatureSecret);
    @Autowired
    ObjectMapper objectMapper;
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
    @MockBean
    ProjectService projectService;
    @MockBean
    CloneDetectionService cloneDetectionService;
    @MockBean
    CurrentUserRepo currentUserRepo;

    @BeforeEach
    public void setUp() {
        client = WebTestClient
            .bindToRouterFunction(webhookRoute)
            .configureClient()
            .codecs(c -> c.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON)))
            .build();
    }

    @Test
    void testPing() {
        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PING)
            .header(GITHUB_SIGNATURE, pingSignature)
            .bodyValue(pingBody)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testMissingEvent() {
        client.post().uri(API_WEBHOOK_ENDPOINT)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(MISSING_EVENT));
    }

    @Test
    void testMissingSignature() {
        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PING)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(MISSING_SIGNATURE));
    }

    @Test
    void testInvalidSignature() {
        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PING)
            .header(GITHUB_SIGNATURE, "signature")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(INVALID_SIGNATURE));
    }

    @Test
    void testSignatureVerificationFailed() {
        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PING)
            .header(GITHUB_SIGNATURE, pingSignature)
            .bodyValue(pingBody + "salt")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(SIGNATURE_VERIFICATION_FAILED));
    }

    @Test
    void testNotSupportedEvent() {
        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, "NOT_YET_SUPPORTED_EVENT")
            .header(GITHUB_SIGNATURE, pingSignature)
            .bodyValue(pingBody)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(NOT_SUPPORTED_EVENT));
    }

    @Test
    void testAssigned() {
        when(projectService.updatePullInfo(any(GithubApiPull.class)))
            .thenReturn(Mono.just(GithubApiToModelConverter.convert(assignedPayload.pull())));

        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PULL)
            .header(GITHUB_SIGNATURE, assignedPayloadSignature)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(assignedPayload)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testUnassignedErrorDuringPayloadProcessing() {
        when(projectService.updatePullInfo(any(GithubApiPull.class)))
            .thenReturn(Mono.error(new IllegalStateException()));

        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PULL)
            .header(GITHUB_SIGNATURE, unassignedPayloadSignature)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(unassignedPayload)
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    void testPushKeepFilesSyncedDisabled() {
        when(projectRepo.idByRepoId(any()))
            .thenReturn(Mono.just(1L));
        when(projectRepo.confById(any()))
            .thenReturn(Mono.just(Project.Conf.DEFAULT.withExcludedFiles(List.of("f1", "f2"))));

        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PUSH)
            .header(GITHUB_SIGNATURE, pushPayloadSignature)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(pushPayload)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testPushKeepFilesSyncedEnabled() {
        final var files = List.of(Project.Conf.KEEP_EXCLUDED_FILES_SYNCED, "f1", "f2");
        when(projectRepo.idByRepoId(any()))
            .thenReturn(Mono.just(1L));
        when(projectRepo.confById(any()))
            .thenReturn(Mono.just(Project.Conf.DEFAULT.withExcludedFiles(List.of(Project.Conf.KEEP_EXCLUDED_FILES_SYNCED))));
        when(projectService.headFiles(any()))
            .thenReturn(Mono.just(files));
        when(projectRepo.upsertConf(any(), any()))
            .thenReturn(Mono.empty());

        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PUSH)
            .header(GITHUB_SIGNATURE, pushPayloadSignature)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(pushPayload)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testPullOpenedOk() {
        final var pull = GithubApiToModelConverter.convert(GithubWebhookRouterTest.pullOpenedPayload.pull());

        when(projectRepo.idByRepoId(anyLong()))
            .thenReturn(Mono.just(1L));
        when(projectService.init(any(GithubApiPull.class)))
            .thenReturn(Mono.just(PullSnapshots.of(pull, List.of(pull.head()))));
        when(cloneDetectionService.detectClonesInNewFilesAndSaveToDb(anyLong(), any(Pull.class), anyIterable()))
            .thenReturn(Flux.empty());

        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PULL)
            .header(GITHUB_SIGNATURE, openedPayloadSignature)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(pullOpenedPayload)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testPullSynchronizeOk() {
        final var pull = GithubApiToModelConverter.convert(GithubWebhookRouterTest.pullSynchronizePayload.pull());

        when(projectRepo.idByRepoId(anyLong()))
            .thenReturn(Mono.just(1L));
        when(projectService.updateWithNewCommits(any(GithubApiPull.class)))
            .thenReturn(Mono.just(PullSnapshots.of(pull, List.of(pull.head(), pull.base()))));
        when(cloneDetectionService.detectClonesInNewFilesAndSaveToDb(anyLong(), any(Pull.class), anyIterable()))
            .thenReturn(Flux.empty());

        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PULL)
            .header(GITHUB_SIGNATURE, synchronizePayloadSignature)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(pullSynchronizePayload)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testNotSupportedPullEventAction() {
        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PULL)
            .header(GITHUB_SIGNATURE, convertedToDraftPayloadSignature)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(pullConvertedToDraftPayload)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testUnableToDeserializePayload() {
        final var payloadJson = """
            {
              "action": "hello_world"
            }
            """;
        client.post().uri(API_WEBHOOK_ENDPOINT)
            .header(GITHUB_EVENT, GITHUB_EVENT_PULL)
            .header(GITHUB_SIGNATURE,  "sha256=" + Signatures.signHexHmacSha256(payloadJson, signatureSecret))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payloadJson)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class).isEqualTo(ApiError.with(UNABLE_TO_DESERIALIZE_PAYLOAD));
    }
}
