package org.accula.api.routers;

import lombok.SneakyThrows;
import org.accula.api.config.WebhookProperties;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.api.GithubClientException;
import org.accula.api.github.model.GithubApiCommitSnapshot;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiPull.State;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.github.model.GithubApiUser;
import org.accula.api.handlers.ProjectsHandler;
import org.accula.api.handlers.dto.ProjectDto;
import org.accula.api.handlers.request.CreateProjectRequestBody;
import org.accula.api.handlers.request.RequestBody;
import org.accula.api.handlers.util.ProjectUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@WebFluxTest
@ContextConfiguration(classes = {ProjectsHandler.class, ProjectsRouter.class, GithubApiToModelConverter.class, ModelToDtoConverter.class, WebhookProperties.class})
public class ProjectsRouterTest {
    private static final String REPO_URL = "https://github.com/accula/accula";
    private static final String REPO_NAME = "accula";
    private static final String REPO_OWNER = "accula";

    private static final Pull PULL = Pull.builder()
            .id(1L)
            .projectId(1L)
            .open(true)
            .head(CommitSnapshot.builder().build())
            .base(CommitSnapshot.builder().build())
            .build();
    private static final List<Pull> PULLS = List.of(PULL, PULL, PULL);
    private static final String EMPTY = "";
    private static final GithubUser GITHUB_USER = new GithubUser(1L, "login", "name", "avatar", false);
    private static final User CURRENT_USER = new User(0L, "", GITHUB_USER);
    private static final GithubApiUser GH_OWNER = new GithubApiUser(1L, REPO_OWNER, EMPTY, EMPTY, EMPTY, GithubApiUser.Type.USER);
    private static final GithubApiRepo GH_REPO = new GithubApiRepo(1L, REPO_URL, REPO_NAME, EMPTY, GH_OWNER);
    private static final GithubApiCommitSnapshot MARKER = new GithubApiCommitSnapshot("", "", GH_OWNER, GH_REPO, "");
    private static final GithubApiPull GH_PULL = new GithubApiPull(0L, "", MARKER, MARKER, GH_OWNER, 0, "", State.OPEN, Instant.now(), Instant.now());
    private static final GithubApiPull[] OPEN_PULLS = new GithubApiPull[]{GH_PULL, GH_PULL, GH_PULL};
    private static final GithubRepo REPO = new GithubRepo(1L, "name", "description", GITHUB_USER);
    private static final Project PROJECT = Project.builder().id(1L).githubRepo(REPO).creator(CURRENT_USER).openPullCount(OPEN_PULLS.length).build();
    private static final RequestBody REQUEST_BODY = new CreateProjectRequestBody(REPO_URL);
    private static final String INVALID_REPO_URL = "htps://bad_url";
    private static final RequestBody REQUEST_BODY_INVALID_URL = new CreateProjectRequestBody(INVALID_REPO_URL);
    private static final String ERROR_FORMAT = "{\"error\":\"%s\"}";
    private static final GithubClientException GH_EXCEPTION = newGithubException();

    @MockBean
    private ProjectUpdater projectUpdater;
    @MockBean
    private GithubUserRepo githubUserRepo;
    @MockBean
    private CurrentUserRepo currentUser;
    @MockBean
    private ProjectRepo projectRepo;
    @MockBean
    private UserRepo userRepo;
    @MockBean
    private PullRepo pullRepo;
    @MockBean
    private GithubClient githubClient;
    @Autowired
    private ModelToDtoConverter converter;
    @Autowired
    private RouterFunction<ServerResponse> projectsRoute;
    private WebTestClient client;

    @BeforeEach
    public void setUp() {
        client = WebTestClient
                .bindToRouterFunction(projectsRoute)
                .build();
    }

    @Test
    public void testCreateProjectSuccess() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        Mockito.when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        Mockito.when(projectRepo.upsert(Mockito.any(GithubRepo.class), Mockito.any(User.class)))
                .thenReturn(Mono.just(PROJECT));

        Mockito.when(projectRepo.notExists(Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(pullRepo.upsert(Mockito.anyCollection()))
                .thenReturn(Flux.fromIterable(PULLS));

        Mockito.when(projectUpdater.update(Mockito.anyLong(), Mockito.any(GithubApiPull[].class)))
                .thenReturn(Mono.just(OPEN_PULLS.length));

        Mockito.when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName(), State.ALL))
                .thenReturn(Mono.just(OPEN_PULLS));

        Mockito.when(githubClient.createHook(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        final var expectedBody = converter.convert(PROJECT);

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectDto.class).isEqualTo(expectedBody);

    }

    @Test
    public void testCreateProjectFailureInvalidUrl() {
        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY_INVALID_URL)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo(String.format(ERROR_FORMAT, "INVALID_URL"));
    }

    @Test
    public void testCreateProjectFailureWrongUrl() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        // simulate github client error that is usually caused by wrong url
        Mockito.when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.error(GH_EXCEPTION));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.error(GH_EXCEPTION));

        Mockito.when(githubClient.getRepositoryPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName(), State.ALL))
                .thenReturn(Mono.error(GH_EXCEPTION));

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo(String.format(ERROR_FORMAT, "WRONG_URL"));
    }

    @Test
    public void testCreateProjectFailureAlreadyExists() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        // make repo existing
        Mockito.when(projectRepo.notExists(Mockito.anyLong()))
                .thenReturn(Mono.just(FALSE));

        Mockito.when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName(), State.ALL))
                .thenReturn(Mono.just(OPEN_PULLS));

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo(String.format(ERROR_FORMAT, "ALREADY_EXISTS"));

    }

    @Test
    public void testCreateProjectFailureNoPermission() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        // disable admin permission
        Mockito.when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(FALSE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName(), State.ALL))
                .thenReturn(Mono.just(OPEN_PULLS));

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(String.class).isEqualTo(String.format(ERROR_FORMAT, "NO_PERMISSION"));

    }

    @Test
    public void testGetProjectSuccess() {
        Mockito.when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        Mockito.when(projectRepo.findById(Mockito.anyLong()))
                .thenReturn(Mono.just(PROJECT));

        final var expectedBody = ModelToDtoConverter.convert(PROJECT);

        client.get().uri("/api/projects/{id}", PROJECT.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectDto.class).isEqualTo(expectedBody);
    }

    @Test
    public void testGetProjectFailure() {
        Mockito.when(projectRepo.findById(0L))
                .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{id}", 0L)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void testGetAllProjects() {
        Mockito.when(projectRepo.getTop(Mockito.anyInt()))
                .thenReturn(Flux.fromArray(new Project[]{PROJECT, PROJECT}));

        final var expectedBody = ModelToDtoConverter.convert(PROJECT);

        client.get().uri("/api/projects?count={count}", 2L)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectDto[].class).isEqualTo(new ProjectDto[]{expectedBody, expectedBody});
    }

    @Test
    public void testDeleteProject() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        Mockito.when(projectRepo.delete(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

        client.delete().uri("/api/projects/{id}", PROJECT.getId())
                .exchange()
                .expectStatus().isOk();
    }

    @SneakyThrows
    private static GithubClientException newGithubException() {
        final var ctor = GithubClientException.class.getDeclaredConstructor(Throwable.class);
        ctor.setAccessible(true);
        return ctor.newInstance(new RuntimeException());
    }
}
