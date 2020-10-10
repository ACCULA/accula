package org.accula.api.routers;

import lombok.SneakyThrows;
import org.accula.api.code.CodeLoader;
import org.accula.api.config.WebConfig;
import org.accula.api.config.WebhookProperties;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.api.GithubClientException;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiPull.State;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.github.model.GithubApiSnapshot;
import org.accula.api.github.model.GithubApiUser;
import org.accula.api.handlers.ProjectsHandler;
import org.accula.api.handlers.dto.ProjectConfDto;
import org.accula.api.handlers.dto.ProjectDto;
import org.accula.api.handlers.dto.UserDto;
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@WebFluxTest
@ContextConfiguration(classes = {ProjectsHandler.class, ProjectsRouter.class, WebhookProperties.class, WebConfig.class})
class ProjectsRouterTest {
    static final String REPO_URL = "https://github.com/accula/accula";
    static final String REPO_NAME = "accula";
    static final String REPO_OWNER = "accula";

    static final Pull PULL = Pull.builder()
            .id(1L)
            .projectId(1L)
            .open(true)
            .head(Snapshot.builder().build())
            .base(Snapshot.builder().build())
            .build();
    static final List<Pull> PULLS = List.of(PULL, PULL, PULL);
    static final String EMPTY = "";
    static final GithubUser GITHUB_USER = new GithubUser(1L, "login", "name", "avatar", false);
    static final User CURRENT_USER = new User(0L, "", GITHUB_USER);
    static final GithubUser GH_USER_2 = new GithubUser(2L, "l", "n", "a", false);
    static final User USER_2 = new User(1L, "", GH_USER_2);
    static final GithubUser GH_USER_3 = new GithubUser(3L, "l", "n", "a", false);
    static final User USER_3 = new User(2L, "", GH_USER_3);
    static final GithubApiUser GH_OWNER = new GithubApiUser(1L, REPO_OWNER, EMPTY, EMPTY, EMPTY, GithubApiUser.Type.USER);
    static final GithubApiRepo GH_REPO = new GithubApiRepo(1L, REPO_URL, REPO_NAME, EMPTY, GH_OWNER);
    static final GithubApiSnapshot MARKER = new GithubApiSnapshot("", "", GH_OWNER, GH_REPO, "");
    static final GithubApiPull GH_PULL = new GithubApiPull(0L, "", MARKER, MARKER, GH_OWNER, 0, "", State.OPEN, Instant.now(), Instant.now());
    static final GithubApiPull[] OPEN_PULLS = new GithubApiPull[]{GH_PULL, GH_PULL, GH_PULL};
    static final GithubRepo REPO = new GithubRepo(1L, "name", "description", GITHUB_USER);
    static final Project PROJECT = Project.builder().id(1L).githubRepo(REPO).creator(CURRENT_USER).openPullCount(OPEN_PULLS.length).build();
    static final RequestBody REQUEST_BODY = new CreateProjectRequestBody(REPO_URL);
    static final String INVALID_REPO_URL = "htps://bad_url";
    static final RequestBody REQUEST_BODY_INVALID_URL = new CreateProjectRequestBody(INVALID_REPO_URL);
    static final String ERROR_FORMAT = "{\"error\":\"%s\"}";
    static final GithubClientException GH_EXCEPTION = newGithubException();

    @MockBean
    ProjectUpdater projectUpdater;
    @MockBean
    GithubUserRepo githubUserRepo;
    @MockBean
    CurrentUserRepo currentUser;
    @MockBean
    ProjectRepo projectRepo;
    @MockBean
    UserRepo userRepo;
    @MockBean
    PullRepo pullRepo;
    @MockBean
    GithubClient githubClient;
    @Autowired
    RouterFunction<ServerResponse> projectsRoute;
    WebTestClient client;
    @MockBean
    CodeLoader codeLoader;

    @BeforeEach
    void setUp() {
        client = WebTestClient
                .bindToRouterFunction(projectsRoute)
                .build();
    }

    @Test
    void testCreateProjectSuccess() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        Mockito.when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        Mockito.when(projectRepo.upsert(Mockito.any(GithubRepo.class), Mockito.any(User.class)))
                .thenReturn(Mono.just(PROJECT));

        Mockito.when(projectRepo.notExists(Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(projectRepo.upsertConf(Mockito.anyLong(), Mockito.any(Project.Conf.class)))
                .thenReturn(Mono.just(Project.Conf.DEFAULT));

        Mockito.when(pullRepo.upsert(Mockito.anyCollection()))
                .thenReturn(Flux.fromIterable(PULLS));

        Mockito.when(projectUpdater.update(Mockito.anyLong(), Mockito.anyList()))
                .thenReturn(Mono.just(OPEN_PULLS.length));

        Mockito.when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName(), State.ALL, 100))
                .thenReturn(Flux.fromArray(OPEN_PULLS));

        Mockito.when(githubClient.createHook(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        final var expectedBody = ModelToDtoConverter.convert(PROJECT);

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProjectDto.class).isEqualTo(expectedBody);

    }

    @Test
    void testCreateProjectFailureInvalidUrl() {
        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY_INVALID_URL)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo(String.format(ERROR_FORMAT, "INVALID_URL"));
    }

    @Test
    void testCreateProjectFailureWrongUrl() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        // simulate github client error that is usually caused by wrong url
        Mockito.when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.error(GH_EXCEPTION));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.error(GH_EXCEPTION));

        Mockito.when(githubClient.getRepositoryPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName(), State.ALL, 100))
                .thenReturn(Flux.error(GH_EXCEPTION));

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo(String.format(ERROR_FORMAT, "WRONG_URL"));
    }

    @Test
    void testCreateProjectFailureAlreadyExists() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        // make repo existing
        Mockito.when(projectRepo.notExists(Mockito.anyLong()))
                .thenReturn(Mono.just(FALSE));

        Mockito.when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(TRUE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName(), State.ALL, 100))
                .thenReturn(Flux.fromArray(OPEN_PULLS));

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class).isEqualTo(String.format(ERROR_FORMAT, "ALREADY_EXISTS"));

    }

    @Test
    void testCreateProjectFailureNoPermission() {
        Mockito.when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        // disable admin permission
        Mockito.when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(FALSE));

        Mockito.when(githubClient.getRepo(GH_REPO.getOwner().getLogin(), GH_REPO.getName()))
                .thenReturn(Mono.just(GH_REPO));

        Mockito.when(githubClient.getRepositoryPulls(GH_REPO.getOwner().getLogin(), GH_REPO.getName(), State.ALL, 100))
                .thenReturn(Flux.fromArray(OPEN_PULLS));

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(String.class).isEqualTo(String.format(ERROR_FORMAT, "NO_PERMISSION"));

    }

    @Test
    void testGetProjectSuccess() {
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
    void testGetProjectFailure() {
        Mockito.when(projectRepo.findById(0L))
                .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{id}", 0L)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testGetAllProjects() {
        Mockito.when(projectRepo.getTop(Mockito.anyInt()))
                .thenReturn(Flux.fromArray(new Project[]{PROJECT, PROJECT}));

        final var expectedBody = ModelToDtoConverter.convert(PROJECT);

        client.get().uri("/api/projects?count={count}", 2L)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectDto[].class).isEqualTo(new ProjectDto[]{expectedBody, expectedBody});
    }

    @Test
    void testDeleteProject() {
        Mockito.when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));

        Mockito.when(projectRepo.delete(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

        client.delete().uri("/api/projects/{id}", PROJECT.getId())
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void testGetGithubAdmins() {
        Mockito.when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        Mockito.when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        Mockito.when(projectRepo.findById(Mockito.anyLong()))
                .thenReturn(Mono.just(PROJECT));
        Mockito.when(githubClient.getRepoAdmins(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(List.of(1L, 2L)));
        Mockito.when(userRepo.findByGithubIds(Mockito.anyCollection()))
                .thenReturn(Flux.fromIterable(List.of(USER_2, USER_3)));

        client.get().uri("/api/projects/{id}/githubAdmins", PROJECT.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto[].class).value(it -> assertEquals(2, it.length));
    }

    @Test
    void testGetGithubAdminsForbidden() {
        mockForbidden();

        client.get().uri("/api/projects/{id}/githubAdmins", PROJECT.getId())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void testGetGithubAdminsNotFound() {
        mockNotFound();

        client.get().uri("/api/projects/{id}/githubAdmins", PROJECT.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testGetConf() {
        Mockito.when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        Mockito.when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        final var adminIds = List.of(1L, 2L);
        Mockito.when(projectRepo.confById(Mockito.anyLong()))
                .thenReturn(Mono.just(Project.Conf.DEFAULT.toBuilder().adminIds(adminIds).build()));

        client.get().uri("/api/projects/{id}/conf", PROJECT.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectConfDto.class)
                .isEqualTo(ProjectConfDto.builder()
                        .admins(adminIds)
                        .cloneMinTokenCount(Project.Conf.DEFAULT.getCloneMinTokenCount())
                        .fileMinSimilarityIndex(Project.Conf.DEFAULT.getFileMinSimilarityIndex())
                        .excludedFiles(Project.Conf.DEFAULT.getExcludedFiles())
                        .build());
    }

    @Test
    void testGetConfForbidden() {
        mockForbidden();

        client.get().uri("/api/projects/{id}/conf", PROJECT.getId())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void testGetConfNotFound() {
        mockNotFound();
        Mockito.when(projectRepo.confById(Mockito.anyLong()))
                .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{id}/conf", PROJECT.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testPutConf() {
        Mockito.when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        Mockito.when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        final var adminIds = List.of(1L, 2L);
        Mockito.when(projectRepo.upsertConf(Mockito.anyLong(), Mockito.any(Project.Conf.class)))
                .thenReturn(Mono.just(Project.Conf.DEFAULT.toBuilder().adminIds(adminIds).build()));

        client.put().uri("/api/projects/{id}/conf", PROJECT.getId())
                .contentType(APPLICATION_JSON)
                .bodyValue(ProjectConfDto.builder()
                        .admins(adminIds)
                        .cloneMinTokenCount(Project.Conf.DEFAULT.getCloneMinTokenCount())
                        .fileMinSimilarityIndex(Project.Conf.DEFAULT.getFileMinSimilarityIndex())
                        .excludedFiles(Project.Conf.DEFAULT.getExcludedFiles())
                        .build())
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testPutConfBadRequest() {
        Mockito.when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        Mockito.when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

        client.put().uri("/api/projects/{id}/conf", PROJECT.getId())
                .contentType(APPLICATION_JSON)
                .bodyValue(ProjectConfDto.builder().build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testHeadFiles() {
        final var expectedFiles = new String[]{"file1", "file2", "file3"};
        Mockito.when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        Mockito.when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        Mockito.when(projectRepo.findById(Mockito.anyLong()))
                .thenReturn(Mono.just(PROJECT));
        Mockito.when(codeLoader.loadFilenames(Mockito.any()))
                .thenReturn(Flux.fromArray(expectedFiles));

        client.get().uri("/api/projects/{id}/headFiles", PROJECT.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String[].class).value(actualFiles -> assertArrayEquals(expectedFiles, actualFiles));
    }

    @Test
    void testHeadFilesForbidden() {
        mockForbidden();

        client.get().uri("/api/projects/{id}/headFiles", PROJECT.getId())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void testHeadFilesNotFound() {
        mockNotFound();

        client.get().uri("/api/projects/{id}/headFiles", PROJECT.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @SneakyThrows
    private static GithubClientException newGithubException() {
        final var ctor = GithubClientException.class.getDeclaredConstructor(Throwable.class);
        ctor.setAccessible(true);
        return ctor.newInstance(new RuntimeException());
    }

    private void mockNotFound() {
        Mockito.when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        Mockito.when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        Mockito.when(projectRepo.findById(Mockito.anyLong()))
                .thenReturn(Mono.empty());
    }

    private void mockForbidden() {
        Mockito.when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        Mockito.when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(FALSE));
    }
}
