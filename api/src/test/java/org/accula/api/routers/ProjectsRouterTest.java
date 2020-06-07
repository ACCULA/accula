package org.accula.api.routers;

import lombok.SneakyThrows;
import org.accula.api.config.WebhookProperties;
import org.accula.api.converter.DataConverter;
import org.accula.api.db.CommitRepository;
import org.accula.api.db.PullRepository;
import org.accula.api.db.model.CommitOld;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.PullOld;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.api.GithubClientException;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiPull.State;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.github.model.GithubApiUser;
import org.accula.api.handlers.ProjectsHandler;
import org.accula.api.handlers.request.CreateProjectRequestBody;
import org.accula.api.handlers.request.RequestBody;
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
@ContextConfiguration(classes = {ProjectsHandler.class, ProjectsRouter.class, DataConverter.class})
public class ProjectsRouterTest {
    private static final String REPO_URL = "https://github.com/accula/accula";
    private static final String REPO_NAME = "accula";
    private static final String REPO_OWNER = "accula";
    private static final List<CommitOld> COMMITS = List.of(new CommitOld(), new CommitOld(), new CommitOld());
    private static final List<PullOld> PULLS = List.of(
            new PullOld(null, 0L, null, null, null, null),
            new PullOld(null, 0L, null, null, null, null),
            new PullOld(null, 0L, null, null, null, null));
    private static final String EMPTY = "";
    private static final User[] ADMINS = new User[0];
    private static final GithubUser GITHUB_USER = new GithubUser(1L, "login", "name", "avatar", false);
    private static final User CURRENT_USER = new User(0L, GITHUB_USER, "token");
    private static final GithubApiUser GH_OWNER = new GithubApiUser(1L, REPO_OWNER, EMPTY, EMPTY, EMPTY, GithubApiUser.Type.USER);
    private static final GithubApiRepo GH_REPO = new GithubApiRepo(1L, REPO_URL, REPO_NAME, EMPTY, GH_OWNER);
    private static final GithubApiPull.Marker MARKER = new GithubApiPull.Marker("", "", GH_REPO, "");
    private static final GithubApiPull PULL = new GithubApiPull(null, MARKER, MARKER, GH_OWNER, 0, "", State.OPEN, Instant.now(), Instant.now());
    private static final GithubApiPull[] OPEN_PULLS = new GithubApiPull[]{PULL, PULL, PULL};
    private static final GithubRepo REPO = new GithubRepo(1L, "name", GITHUB_USER, "description");
    private static final Project PROJECT = new Project(1L, REPO, CURRENT_USER, ADMINS);
    private static final RequestBody REQUEST_BODY = new CreateProjectRequestBody(REPO_URL);
    private static final String INVALID_REPO_URL = "htps://bad_url";
    private static final RequestBody REQUEST_BODY_INVALID_URL = new CreateProjectRequestBody(INVALID_REPO_URL);
    private static final String ERROR_FORMAT = "{\"error\":\"%s\"}";
    private static final GithubClientException GH_EXCEPTION = newGithubException();

    @MockBean
    private GithubUserRepo githubUserRepo;
    @MockBean
    private CurrentUserRepo currentUser;
    @MockBean
    private ProjectRepo projectRepo;
    @MockBean
    private PullRepository pullRepository;
    @MockBean
    private CommitRepository commitRepo;
    @MockBean
    private GithubClient githubClient;
    @MockBean
    private WebhookProperties webhookProperties;
    @Autowired
    private DataConverter converter;
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

        Mockito.when(commitRepo.saveAll(Mockito.anyCollection()))
                .thenReturn(Flux.fromIterable(COMMITS));

        Mockito.when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        Mockito.when(projectRepo.upsert(Mockito.any(GithubRepo.class), Mockito.any(User.class)))
                .thenReturn(Mono.just(PROJECT));

        Mockito.when(projectRepo.notExists(Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(pullRepository.saveAll(Mockito.any(Flux.class)))
                .thenReturn(Flux.fromIterable(PULLS));

        Mockito.when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName(), State.ALL))
                .thenReturn(Mono.just(OPEN_PULLS));

        Mockito.when(githubClient.createHook(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class).isEqualTo(PROJECT);

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

        Mockito.when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        Mockito.when(projectRepo.notExists(Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

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

        Mockito.when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        Mockito.when(projectRepo.upsert(Mockito.any(GithubRepo.class), Mockito.any(User.class)))
                .thenReturn(Mono.just(PROJECT));

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

        Mockito.when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        Mockito.when(projectRepo.upsert(Mockito.any(GithubRepo.class), Mockito.any(User.class)))
                .thenReturn(Mono.just(PROJECT));

        Mockito.when(projectRepo.notExists(Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

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

        Mockito.when(projectRepo.upsert(Mockito.any(GithubRepo.class), Mockito.any(User.class)))
                .thenReturn(Mono.just(PROJECT));

        client.get().uri("/api/projects/{id}", PROJECT.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class).isEqualTo(PROJECT);
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
        Mockito.when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        Mockito.when(projectRepo.getTop(5))
                .thenReturn(Flux.fromArray(new Project[]{PROJECT, PROJECT, PROJECT}));

        client.get().uri("/api/projects?count={count}", 2L)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project[].class).isEqualTo(new Project[]{PROJECT, PROJECT});
    }

    @Test
    public void testDeleteProject() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        Mockito.when(projectRepo.delete(PROJECT.getId()))
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
