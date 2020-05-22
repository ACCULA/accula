package org.accula.api.routers;

import lombok.SneakyThrows;
import org.accula.api.db.CurrentUserRepository;
import org.accula.api.db.ProjectRepository;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.api.GithubClientException;
import org.accula.api.github.model.GithubOwner;
import org.accula.api.github.model.GithubPull;
import org.accula.api.github.model.GithubRepo;
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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@WebFluxTest
@ContextConfiguration(classes = {ProjectsHandler.class, ProjectsRouter.class})
public class ProjectsRouterTest {
    private static final String REPO_URL = "https://github.com/accula/accula";
    private static final String REPO_NAME = "accula";
    private static final String REPO_OWNER = "accula";
    private static final GithubPull[] OPEN_PULLS = new GithubPull[]{new GithubPull(), new GithubPull(), new GithubPull()};
    private static final String EMPTY = "";
    private static final Long[] ADMINS = new Long[]{1L, 2L, 3L};
    private static final User CURRENT_USER = new User(0L, "Steve", 123L, "jobs", "secret_token");
    private static final Project PROJECT = new Project(0L, CURRENT_USER.getId(), REPO_URL, REPO_NAME, EMPTY, OPEN_PULLS.length, REPO_OWNER, EMPTY, ADMINS);
    private static final GithubOwner GH_OWNER = new GithubOwner(REPO_OWNER, EMPTY);
    private static final GithubRepo GH_REPO = new GithubRepo(REPO_URL, REPO_NAME, EMPTY, GH_OWNER);
    private static final RequestBody REQUEST_BODY = new CreateProjectRequestBody(REPO_URL);
    private static final String INVALID_REPO_URL = "htps://bad_url";
    private static final RequestBody REQUEST_BODY_INVALID_URL = new CreateProjectRequestBody(INVALID_REPO_URL);
    private static final String ERROR_FORMAT = "{\"error\":\"%s\"}";
    private static final GithubClientException GH_EXCEPTION = newGithubException();

    @MockBean
    private CurrentUserRepository currentUser;
    @MockBean
    private ProjectRepository repository;
    @MockBean
    private GithubClient githubClient;
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

        Mockito.when(repository.save(Mockito.any(Project.class)))
                .thenReturn(Mono.just(PROJECT));

        Mockito.when(repository.notExistsByRepoOwnerAndRepoName(REPO_OWNER, REPO_NAME))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(githubClient.hasAdminPermission(PROJECT.getRepoOwner(), PROJECT.getRepoName()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryOpenPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(OPEN_PULLS));

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

        Mockito.when(repository.notExistsByRepoOwnerAndRepoName(REPO_OWNER, REPO_NAME))
                .thenReturn(Mono.just(TRUE));

        // simulate github client error that is usually caused by wrong url
        Mockito.when(githubClient.hasAdminPermission(PROJECT.getRepoOwner(), PROJECT.getRepoName()))
                .thenReturn(Mono.error(GH_EXCEPTION));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.error(GH_EXCEPTION));

        Mockito.when(githubClient.getRepositoryOpenPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
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

        Mockito.when(repository.save(Mockito.any(Project.class)))
                .thenReturn(Mono.just(PROJECT));

        // make repo existing
        Mockito.when(repository.notExistsByRepoOwnerAndRepoName(REPO_OWNER, REPO_NAME))
                .thenReturn(Mono.just(FALSE));

        Mockito.when(githubClient.hasAdminPermission(PROJECT.getRepoOwner(), PROJECT.getRepoName()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryOpenPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
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

        Mockito.when(repository.save(Mockito.any(Project.class)))
                .thenReturn(Mono.just(PROJECT));

        Mockito.when(repository.notExistsByRepoOwnerAndRepoName(REPO_OWNER, REPO_NAME))
                .thenReturn(Mono.just(TRUE));

        // disable admin permission
        Mockito.when(githubClient.hasAdminPermission(PROJECT.getRepoOwner(), PROJECT.getRepoName()))
                .thenReturn(Mono.just(FALSE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryOpenPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
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
        Mockito.when(repository.findById(PROJECT.getId()))
                .thenReturn(Mono.just(PROJECT));

        client.get().uri("/api/projects/{id}", PROJECT.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class).isEqualTo(PROJECT);
    }

    @Test
    public void testGetProjectFailure() {
        Mockito.when(repository.findById(0L))
                .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{id}", 0L)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void testGetAllProjects() {
        Mockito.when(repository.findAll())
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

        Mockito.when(repository.deleteByIdAndCreatorId(PROJECT.getId(), PROJECT.getCreatorId()))
                .thenReturn(Mono.just(TRUE));

        client.delete().uri("/api/projects/{id}", PROJECT.getId())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testUpdateProject() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        final var adminsHolder = new Object() {
            Long[] admins;
        };
        final var admins = new Long[]{1L, 3L, 5L};

        Mockito.when(repository.setAdmins(PROJECT.getId(), admins, PROJECT.getCreatorId()))
                .thenReturn(Mono.fromRunnable(() -> adminsHolder.admins = admins));

        final var adminsUpdate = new Project();
        adminsUpdate.setAdmins(admins);
        adminsUpdate.setId(PROJECT.getId());

        client.put().uri("/api/projects/{id}", PROJECT.getId())
                .bodyValue(adminsUpdate)
                .exchange()
                .expectStatus().isOk();

        assertArrayEquals(admins, adminsHolder.admins);
    }

    @SneakyThrows
    private static GithubClientException newGithubException() {
        final var ctor = GithubClientException.class.getDeclaredConstructor(Throwable.class);
        ctor.setAccessible(true);
        return ctor.newInstance(new RuntimeException());
    }
}
