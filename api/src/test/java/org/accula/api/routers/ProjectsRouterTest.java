package org.accula.api.routers;

import lombok.SneakyThrows;
import org.accula.api.code.CodeLoader;
import org.accula.api.config.WebConfig;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubRepoRepo;
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
import org.accula.api.handler.ProjectsHandler;
import org.accula.api.handler.dto.AddRepoDto;
import org.accula.api.handler.dto.ApiError;
import org.accula.api.handler.dto.CreateProjectDto;
import org.accula.api.handler.dto.ProjectConfDto;
import org.accula.api.handler.dto.ProjectDto;
import org.accula.api.handler.dto.RepoShortDto;
import org.accula.api.handler.dto.UserDto;
import org.accula.api.handler.exception.ProjectsHandlerException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.service.ProjectService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
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
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@WebFluxTest
@ContextConfiguration(classes = {WebConfig.class, ProjectsHandler.class, ProjectsRouter.class})
class ProjectsRouterTest {
    static final String REPO_URL = "https://github.com/accula/accula";
    static final String REPO_NAME = "accula";
    static final String REPO_OWNER = "accula";
    static final String REPO_OWNER_HIGHLOAD = "polis-mail-ru";
    static final String REPO_NAME_HIGHLOAD1 = "2020-highload-dht";
    static final String REPO_NAME_HIGHLOAD2 = "2019-highload-dht";
    static final String REPO_NAME_HIGHLOAD3 = "2018-highload-kv";
    static final String REPO_NAME_HIGHLOAD4 = "2021-highload-dht";
    static final String REPO_URL_HIGHLOAD1 = "https://github.com/%s/%s".formatted(REPO_OWNER_HIGHLOAD, REPO_NAME_HIGHLOAD1);
    static final String REPO_URL_HIGHLOAD2 = "https://github.com/%s/%s".formatted(REPO_OWNER_HIGHLOAD, REPO_NAME_HIGHLOAD2);
    static final String REPO_URL_HIGHLOAD3 = "https://github.com/%s/%s".formatted(REPO_OWNER_HIGHLOAD, REPO_NAME_HIGHLOAD3);

    static final GithubUser GITHUB_USER = new GithubUser(1L, "accula", "name", "avatar", false);
    static final GithubRepo REPO = new GithubRepo(1L, "accula", "description", GITHUB_USER);
    static final Pull PULL = Pull.builder()
            .id(1L)
            .number(2)
            .isOpen(true)
            .createdAt(Instant.MIN)
            .updatedAt(Instant.EPOCH)
            .author(GITHUB_USER)
            .title("title")
            .head(Snapshot.builder().repo(REPO).branch("branch1").build())
            .base(Snapshot.builder().repo(REPO).branch("branch2").build())
            .primaryProjectId(1L)
            .build();
    static final GithubUser GITHUB_USER_HIGHLOAD = new GithubUser(1L, REPO_OWNER_HIGHLOAD, REPO_OWNER_HIGHLOAD, "avatar", false);
    static final GithubRepo REPO_HIGHLOAD = new GithubRepo(1L, REPO_NAME_HIGHLOAD4, "description", GITHUB_USER_HIGHLOAD);
    static final List<Pull> PULLS = List.of(PULL, PULL, PULL);
    static final String EMPTY = "";
    static final User CURRENT_USER = new User(0L, "", GITHUB_USER);
    static final GithubUser GH_USER_2 = new GithubUser(2L, "l", "n", "a", false);
    static final User USER_2 = new User(1L, "", GH_USER_2);
    static final GithubUser GH_USER_3 = new GithubUser(3L, "l", "n", "a", false);
    static final User USER_3 = new User(2L, "", GH_USER_3);
    static final GithubApiUser GH_OWNER = new GithubApiUser(1L, REPO_OWNER, EMPTY, EMPTY, EMPTY, GithubApiUser.Type.USER);
    static final GithubApiUser GH_OWNER_HIGHLOAD = new GithubApiUser(1L, REPO_OWNER_HIGHLOAD, EMPTY, EMPTY, EMPTY, GithubApiUser.Type.USER);
    static final GithubApiRepo GH_REPO = new GithubApiRepo(1L, REPO_URL, REPO_NAME, EMPTY, GH_OWNER);
    static final GithubApiRepo GH_REPO_HIGHLOAD1 = new GithubApiRepo(2L, REPO_URL_HIGHLOAD1, REPO_NAME_HIGHLOAD1, EMPTY, GH_OWNER_HIGHLOAD);
    static final GithubApiRepo GH_REPO_HIGHLOAD2 = new GithubApiRepo(3L, REPO_URL_HIGHLOAD2, REPO_NAME_HIGHLOAD2, EMPTY, GH_OWNER_HIGHLOAD);
    static final GithubApiRepo GH_REPO_HIGHLOAD3 = new GithubApiRepo(4L, REPO_URL_HIGHLOAD3, REPO_NAME_HIGHLOAD3, EMPTY, GH_OWNER_HIGHLOAD);
    static final GithubApiSnapshot MARKER = new GithubApiSnapshot("", "", GH_OWNER, GH_REPO, "");
    static final GithubApiPull GH_PULL = new GithubApiPull(0L, "", MARKER, MARKER, GH_OWNER, 0, "", State.OPEN, Instant.now(), Instant.now(), Instant.now());
    static final GithubApiPull[] OPEN_PULLS = new GithubApiPull[]{GH_PULL, GH_PULL, GH_PULL};
    static final Project PROJECT = Project.builder().id(1L).state(Project.State.CONFIGURING).githubRepo(REPO).creator(CURRENT_USER).openPullCount(0).build();
    static final Project PROJECT_HIGHLOAD = Project.builder().id(2L).state(Project.State.CONFIGURING).githubRepo(REPO_HIGHLOAD).creator(CURRENT_USER).openPullCount(0).build();
    static final CreateProjectDto REQUEST_BODY = new CreateProjectDto(REPO_URL);
    static final String INVALID_REPO_URL = "htps://bad_url";
    static final CreateProjectDto REQUEST_BODY_INVALID_URL = new CreateProjectDto(INVALID_REPO_URL);
    static final String ERROR_FORMAT = "{\"code\":\"%s\"}";
    static final GithubClientException GH_EXCEPTION = newGithubException();

    @MockBean
    ProjectService projectService;
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
    @MockBean
    CloneDetectionService cloneDetectionService;
    @MockBean
    GithubRepoRepo repoRepo;

    @BeforeEach
    void setUp() {
        client = WebTestClient
                .bindToRouterFunction(projectsRoute)
                .build();
    }

    @Test
    void testCreateProjectSuccess() {
        when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        when(projectRepo.upsert(Mockito.any(GithubRepo.class), Mockito.any(User.class)))
                .thenReturn(Mono.just(PROJECT));

        when(projectRepo.notExists(Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

        when(projectRepo.upsertConf(Mockito.anyLong(), Mockito.any(Project.Conf.class)))
                .thenReturn(Mono.just(Project.Conf.DEFAULT));

        when(pullRepo.upsert(Mockito.anyCollection()))
                .thenReturn(Flux.fromIterable(PULLS));

        when(projectService.init(Mockito.anyList()))
                .thenReturn(Mono.empty());

        when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(TRUE));

        when(githubClient.getRepo(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(GH_REPO));

        when(githubClient.getRepositoryPulls(Mockito.anyString(), Mockito.anyString(), Mockito.any(State.class), Mockito.anyInt()))
                .thenReturn(Flux.fromArray(OPEN_PULLS));

        when(projectRepo.updateState(Mockito.anyLong(), Mockito.any(Project.State.class)))
                .thenReturn(Mono.empty());

        when(githubClient.createHook(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        when(cloneDetectionService.fillSuffixTree(Mockito.anyLong(), Flux.fromIterable(Mockito.anyCollection())))
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
                .expectBody(ApiError.class).isEqualTo(toApiError(ProjectsHandlerException.invalidUrl(INVALID_REPO_URL)));
    }

    @Test
    void testCreateProjectFailureWrongUrl() {
        when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        // simulate github client error that is usually caused by wrong url
        when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.error(GH_EXCEPTION));

        when(githubClient.getRepo(GH_REPO.owner().login(), GH_REPO.name()))
                .thenReturn(Mono.error(GH_EXCEPTION));

        when(githubClient.getRepositoryPulls(GH_REPO.owner().login(), GH_REPO.name(), State.ALL, 100))
                .thenReturn(Flux.error(GH_EXCEPTION));

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class).isEqualTo(toApiError(ProjectsHandlerException.unableRetrieveGithubRepo("accula", "accula")));
    }

    @Test
    void testCreateProjectFailureAlreadyExists() {
        when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        // make repo existing
        when(projectRepo.notExists(Mockito.anyLong()))
                .thenReturn(Mono.just(FALSE));

        when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(TRUE));

        when(githubClient.getRepo(GH_REPO.owner().login(), GH_REPO.name()))
                .thenReturn(Mono.just(GH_REPO));

        when(githubClient.getRepositoryPulls(GH_REPO.owner().login(), GH_REPO.name(), State.ALL, 100))
                .thenReturn(Flux.fromArray(OPEN_PULLS));

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ApiError.class).isEqualTo(toApiError(ProjectsHandlerException.alreadyExists(REPO)));

    }

    @Test
    void testCreateProjectFailureNoPermission() {
        when(currentUser.get())
                .thenReturn(Mono.just(CURRENT_USER));

        // disable admin permission
        when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(FALSE));

        when(githubClient.getRepo(GH_REPO.owner().login(), GH_REPO.name()))
                .thenReturn(Mono.just(GH_REPO));

        when(githubClient.getRepositoryPulls(GH_REPO.owner().login(), GH_REPO.name(), State.ALL, 100))
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
        when(githubUserRepo.upsert(Mockito.any(GithubUser.class)))
                .thenReturn(Mono.just(GITHUB_USER));

        when(projectRepo.findById(Mockito.anyLong()))
                .thenReturn(Mono.just(PROJECT));

        final var expectedBody = ModelToDtoConverter.convert(PROJECT);

        client.get().uri("/api/projects/{id}", PROJECT.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectDto.class).isEqualTo(expectedBody);
    }

    @Test
    void testGetProjectBadRequest() {
        client.get().uri("/api/projects/notANumber")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testGetProjectNotFound() {
        when(projectRepo.findById(0L))
                .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{id}", 0L)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testGetAllProjects() {
        when(projectRepo.getTop(Mockito.anyInt()))
                .thenReturn(Flux.fromArray(new Project[]{PROJECT, PROJECT}));

        final var expectedBody = ModelToDtoConverter.convert(PROJECT);

        client.get().uri("/api/projects?count={count}", 2L)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectDto[].class).isEqualTo(new ProjectDto[]{expectedBody, expectedBody});
    }

    @Test
    void testDeleteProject() {
        when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));

        when(projectRepo.delete(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

        client.delete().uri("/api/projects/{id}", PROJECT.id())
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void testGetGithubAdmins() {
        when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(Mockito.anyLong()))
                .thenReturn(Mono.just(PROJECT));
        when(githubClient.getRepoAdmins(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(List.of(1L, 2L)));
        when(userRepo.findByGithubIds(Mockito.anyCollection()))
                .thenReturn(Flux.fromIterable(List.of(USER_2, USER_3)));

        client.get().uri("/api/projects/{id}/githubAdmins", PROJECT.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto[].class).value(it -> assertEquals(2, it.length));
    }

    @Test
    void testGetGithubAdminsForbidden() {
        mockForbidden();

        client.get().uri("/api/projects/{id}/githubAdmins", PROJECT.id())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void testGetGithubAdminsNotFound() {
        mockNotFound();

        client.get().uri("/api/projects/{id}/githubAdmins", PROJECT.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testGetConf() {
        when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        final var adminIds = List.of(1L, 2L);
        when(projectRepo.confById(Mockito.anyLong()))
                .thenReturn(Mono.just(Project.Conf.DEFAULT.withAdminIds(adminIds)));

        client.get().uri("/api/projects/{id}/conf", PROJECT.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectConfDto.class)
                .isEqualTo(ProjectConfDto.builder()
                        .admins(adminIds)
                        .cloneMinTokenCount(Project.Conf.DEFAULT.cloneMinTokenCount())
                        .fileMinSimilarityIndex(Project.Conf.DEFAULT.fileMinSimilarityIndex())
                        .excludedFiles(Project.Conf.DEFAULT.excludedFiles())
                        .build());
    }

    @Test
    void testGetConfForbidden() {
        mockForbidden();

        client.get().uri("/api/projects/{id}/conf", PROJECT.id())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void testGetConfNotFound() {
        mockNotFound();
        when(projectRepo.confById(Mockito.anyLong()))
                .thenReturn(Mono.empty());

        client.get().uri("/api/projects/{id}/conf", PROJECT.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testPutConf() {
        when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        final var adminIds = List.of(1L, 2L);
        when(projectRepo.upsertConf(Mockito.anyLong(), Mockito.any(Project.Conf.class)))
                .thenReturn(Mono.just(Project.Conf.DEFAULT.withAdminIds(adminIds)));

        client.put().uri("/api/projects/{id}/conf", PROJECT.id())
                .contentType(APPLICATION_JSON)
                .bodyValue(ProjectConfDto.builder()
                        .admins(adminIds)
                        .cloneMinTokenCount(Project.Conf.DEFAULT.cloneMinTokenCount())
                        .fileMinSimilarityIndex(Project.Conf.DEFAULT.fileMinSimilarityIndex())
                        .excludedFiles(Project.Conf.DEFAULT.excludedFiles())
                        .build())
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testPutConfBadRequest() {
        when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));

        client.put().uri("/api/projects/{id}/conf", PROJECT.id())
                .contentType(APPLICATION_JSON)
                .bodyValue(ProjectConfDto.builder().build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testHeadFiles() {
        final var expectedFiles = new String[]{"file1", "file2", "file3"};
        when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(Mockito.anyLong()))
                .thenReturn(Mono.just(PROJECT));
        when(codeLoader.loadFilenames(Mockito.any()))
                .thenReturn(Flux.fromArray(expectedFiles));

        client.get().uri("/api/projects/{id}/headFiles", PROJECT.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String[].class).value(actualFiles -> assertArrayEquals(expectedFiles, actualFiles));
    }

    @Test
    void testHeadFilesForbidden() {
        mockForbidden();

        client.get().uri("/api/projects/{id}/headFiles", PROJECT.id())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void testHeadFilesNotFound() {
        mockNotFound();

        client.get().uri("/api/projects/{id}/headFiles", PROJECT.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testRepoSuggestionOk() {
        when(currentUser.get(Mockito.any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(Mockito.anyLong()))
            .thenReturn(Mono.just(PROJECT_HIGHLOAD));
        when(githubClient.getAllRepos(Mockito.anyInt()))
            .thenReturn(Flux.just(GH_REPO_HIGHLOAD2, GH_REPO, GH_REPO_HIGHLOAD3, GH_REPO_HIGHLOAD1));

        final var suggestion = List.of(
            RepoShortDto.builder()
                .id(GH_REPO_HIGHLOAD1.id())
                .owner(REPO_OWNER_HIGHLOAD)
                .name(REPO_NAME_HIGHLOAD1)
                .build(),
            RepoShortDto.builder()
                .id(GH_REPO_HIGHLOAD2.id())
                .owner(REPO_OWNER_HIGHLOAD)
                .name(REPO_NAME_HIGHLOAD2)
                .build(),
            RepoShortDto.builder()
                .id(GH_REPO_HIGHLOAD3.id())
                .owner(REPO_OWNER_HIGHLOAD)
                .name(REPO_NAME_HIGHLOAD3)
                .build(),
            RepoShortDto.builder()
                .id(GH_REPO.id())
                .owner(REPO_OWNER)
                .name(REPO_NAME)
                .build()
        ).toArray(new RepoShortDto[0]);

        client.get().uri("/api/projects/{id}/repoSuggestion", PROJECT_HIGHLOAD.id())
            .exchange()
            .expectStatus().isOk()
            .expectBody(RepoShortDto[].class).isEqualTo(suggestion);
    }

    @Test
    void testRepoSuggestionForbidden() {
        when(currentUser.get(Mockito.any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(Mono.just(FALSE));
        when(projectRepo.findById(Mockito.anyLong()))
            .thenReturn(Mono.just(PROJECT_HIGHLOAD));
        when(githubClient.getAllRepos(Mockito.anyInt()))
            .thenReturn(Flux.just(GH_REPO_HIGHLOAD2, GH_REPO, GH_REPO_HIGHLOAD3, GH_REPO_HIGHLOAD1));

        client.get().uri("/api/projects/{id}/repoSuggestion", PROJECT_HIGHLOAD.id())
            .exchange()
            .expectStatus().isForbidden()
            .expectBody(Void.class).value(Matchers.nullValue());
    }

    @Test
    void testAddRepoByUrlOk() {
        when(currentUser.get(Mockito.any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(Mockito.anyLong()))
            .thenReturn(Mono.just(PROJECT_HIGHLOAD));
        when(repoRepo.upsert(REPO_HIGHLOAD))
            .thenReturn(Mono.just(REPO_HIGHLOAD));
        when(repoRepo.findByName(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Mono.just(REPO_HIGHLOAD));
        when(projectRepo.attachRepos(Mockito.anyLong(), Mockito.anyCollection()))
            .thenReturn(Mono.empty());
        when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Mono.just(TRUE));
        when(githubClient.getRepo(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Mono.just(GH_REPO));
        when(githubClient.getRepositoryPulls(Mockito.anyString(), Mockito.anyString(), Mockito.any(State.class), Mockito.anyInt()))
            .thenReturn(Flux.fromArray(OPEN_PULLS));
        when(projectRepo.updateState(Mockito.anyLong(), Mockito.any(Project.State.class)))
            .thenReturn(Mono.empty());

        client.post().uri("/api/projects/{id}/addRepoByUrl", PROJECT_HIGHLOAD.id())
            .bodyValue(new AddRepoDto.ByUrl(REPO_URL_HIGHLOAD1))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Void.class).value(Matchers.nullValue());
    }

    @Test
    void testAddRepoForbidden() {
        when(currentUser.get(Mockito.any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(Mono.just(FALSE));

        client.post().uri("/api/projects/{id}/addRepoByUrl", PROJECT_HIGHLOAD.id())
            .bodyValue(new AddRepoDto.ByUrl(REPO_URL_HIGHLOAD1))
            .exchange()
            .expectStatus().isForbidden()
            .expectBody(Void.class).value(Matchers.nullValue());
    }

    @Test
    void testAddRepoByInfoOk() {
        when(currentUser.get(Mockito.any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(Mockito.anyLong()))
            .thenReturn(Mono.just(PROJECT_HIGHLOAD));
        when(repoRepo.upsert(REPO_HIGHLOAD))
            .thenReturn(Mono.just(REPO_HIGHLOAD));
        when(repoRepo.findByName(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Mono.empty());
        when(projectRepo.attachRepos(Mockito.anyLong(), Mockito.anyCollection()))
            .thenReturn(Mono.empty());
        when(githubClient.hasAdminPermission(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Mono.just(TRUE));
        when(githubClient.getRepo(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Mono.just(GH_REPO));
        when(githubClient.getRepositoryPulls(Mockito.anyString(), Mockito.anyString(), Mockito.any(State.class), Mockito.anyInt()))
            .thenReturn(Flux.fromArray(OPEN_PULLS));
        when(projectRepo.updateState(Mockito.anyLong(), Mockito.any(Project.State.class)))
            .thenReturn(Mono.empty());

        client.post().uri("/api/projects/{id}/addRepoByInfo", PROJECT_HIGHLOAD.id())
            .bodyValue(new AddRepoDto.ByInfo(RepoShortDto.builder()
                .id(GH_REPO_HIGHLOAD1.id())
                .owner(REPO_OWNER_HIGHLOAD)
                .name(REPO_NAME_HIGHLOAD1)
                .build()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Void.class).value(Matchers.nullValue());
    }

    private void mockNotFound() {
        when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(Mockito.anyLong()))
                .thenReturn(Mono.empty());
    }

    private void mockForbidden() {
        when(currentUser.get(Mockito.any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Mono.just(FALSE));
    }

    @SneakyThrows
    private static GithubClientException newGithubException() {
        final var ctor = GithubClientException.class.getDeclaredConstructor(Throwable.class);
        ctor.setAccessible(true);
        return ctor.newInstance(new RuntimeException());
    }

    @SneakyThrows
    private ApiError toApiError(ResponseConvertibleException e) {
        final var toApiError = ResponseConvertibleException.class.getDeclaredMethod("toApiError");
        toApiError.setAccessible(true);
        return (ApiError) toApiError.invoke(e);
    }
}
