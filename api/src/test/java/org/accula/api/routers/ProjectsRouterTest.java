package org.accula.api.routers;

import lombok.SneakyThrows;
import org.accula.api.code.CodeLoader;
import org.accula.api.config.WebConfig;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.CodeLanguage;
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
import org.accula.api.handler.dto.ValuesWithSuggestion;
import org.accula.api.handler.exception.HandlerException;
import org.accula.api.handler.exception.ProjectsHandlerException;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.service.ProjectService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.stream.Stream;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.accula.api.util.ApiErrors.toApiError;
import static org.accula.api.util.TestData.acculaAccula;
import static org.accula.api.util.TestData.acculaGithub;
import static org.accula.api.util.TestData.admin;
import static org.accula.api.util.TestData.highload19Project;
import static org.accula.api.util.TestData.lamtev;
import static org.accula.api.util.TestData.user;
import static org.accula.api.util.TestData.user1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@WebFluxTest
@ContextConfiguration(classes = {
    WebConfig.class,
    ProjectsHandler.class,
    ProjectsRouter.class,
})
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

    static final Pull PULL = Pull.builder()
            .id(1L)
            .number(2)
            .isOpen(true)
            .createdAt(Instant.MIN)
            .updatedAt(Instant.EPOCH)
            .author(acculaGithub)
            .title("title")
            .head(Snapshot.builder().repo(acculaAccula).branch("branch1").build())
            .base(Snapshot.builder().repo(acculaAccula).branch("branch2").build())
            .primaryProjectId(1L)
            .build();
    static final GithubUser GITHUB_USER_HIGHLOAD = GithubUser.builder().id(1L).login(REPO_OWNER_HIGHLOAD).name(REPO_OWNER_HIGHLOAD).avatar("avatar").isOrganization(false).build();
    static final GithubRepo REPO_HIGHLOAD = GithubRepo.builder().id(1L).name(REPO_NAME_HIGHLOAD4).isPrivate(false).description("description").owner(GITHUB_USER_HIGHLOAD).build();
    static final List<Pull> PULLS = List.of(PULL, PULL, PULL);
    static final String EMPTY = "";
    static final GithubApiUser GH_OWNER = new GithubApiUser(1L, REPO_OWNER, EMPTY, EMPTY, EMPTY, GithubApiUser.Type.USER);
    static final GithubApiUser GH_OWNER_HIGHLOAD = new GithubApiUser(1L, REPO_OWNER_HIGHLOAD, EMPTY, EMPTY, EMPTY, GithubApiUser.Type.USER);
    static final GithubApiRepo GH_REPO = new GithubApiRepo(1L, REPO_URL, REPO_NAME, false, EMPTY, GH_OWNER);
    static final GithubApiRepo GH_REPO_HIGHLOAD1 = new GithubApiRepo(2L, REPO_URL_HIGHLOAD1, REPO_NAME_HIGHLOAD1, false, EMPTY, GH_OWNER_HIGHLOAD);
    static final GithubApiRepo GH_REPO_HIGHLOAD2 = new GithubApiRepo(3L, REPO_URL_HIGHLOAD2, REPO_NAME_HIGHLOAD2, false, EMPTY, GH_OWNER_HIGHLOAD);
    static final GithubApiRepo GH_REPO_HIGHLOAD3 = new GithubApiRepo(4L, REPO_URL_HIGHLOAD3, REPO_NAME_HIGHLOAD3, false, EMPTY, GH_OWNER_HIGHLOAD);
    static final GithubApiSnapshot MARKER = GithubApiSnapshot.builder().label("").ref("").user(GH_OWNER).repo(GH_REPO).sha("").build();
    static final GithubApiPull GH_PULL = GithubApiPull.builder().id(0L).htmlUrl("").head(MARKER).base(MARKER).user(GH_OWNER).number(0).title("").state(State.OPEN).createdAt(Instant.now()).updatedAt(Instant.now()).mergedAt(Instant.now()).assignee(null).assignees(new GithubApiUser[0]).build();
    static final GithubApiPull[] OPEN_PULLS = new GithubApiPull[]{GH_PULL, GH_PULL, GH_PULL};
    static final Project PROJECT_HIGHLOAD = Project.builder().id(2L).state(Project.State.CONFIGURING).githubRepo(REPO_HIGHLOAD).creator(lamtev).openPullCount(0).build();
    static final CreateProjectDto REQUEST_BODY = new CreateProjectDto(REPO_URL);
    static final String INVALID_REPO_URL = "htps://bad_url";
    static final CreateProjectDto REQUEST_BODY_INVALID_URL = new CreateProjectDto(INVALID_REPO_URL);
    static final String ERROR_FORMAT = "{\"code\":\"%s\"}";
    static final GithubClientException GH_EXCEPTION = newGithubException();

    @Autowired
    RouterFunction<ServerResponse> projectsRoute;

    WebTestClient client;

    @MockBean
    CodeLoader codeLoader;
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
                .thenReturn(Mono.just(lamtev));

        when(githubUserRepo.upsert(any(GithubUser.class)))
                .thenReturn(Mono.just(acculaGithub));

        when(projectRepo.upsert(any(), any()))
                .thenReturn(Mono.just(highload19Project));

        when(projectRepo.notExists(anyLong()))
                .thenReturn(Mono.just(TRUE));

        when(projectRepo.upsertConf(anyLong(), any()))
                .thenReturn(Mono.just(Project.Conf.defaultConf()));

        when(pullRepo.upsert(anyCollection()))
                .thenReturn(Flux.fromIterable(PULLS));

        when(projectService.init(anyList()))
                .thenReturn(Mono.empty());

        when(githubClient.hasAdminPermission(anyString(), anyString()))
                .thenReturn(Mono.just(TRUE));

        when(githubClient.getRepo(anyString(), anyString()))
                .thenReturn(Mono.just(GH_REPO));

        when(githubClient.getRepositoryPulls(anyString(), anyString(), any(), anyInt()))
                .thenReturn(Flux.fromArray(OPEN_PULLS));

        when(projectRepo.updateState(anyLong(), any()))
                .thenReturn(Mono.empty());

        when(githubClient.createHook(any(), any(), any()))
                .thenReturn(Mono.empty());

        when(cloneDetectionService.fillSuffixTree(anyLong(), Flux.fromIterable(anyCollection())))
                .thenReturn(Mono.empty());

        final var expectedBody = ModelToDtoConverter.convert(highload19Project);

        client.post().uri("/api/projects")
                .contentType(APPLICATION_JSON)
                .bodyValue(REQUEST_BODY)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProjectDto.class).isEqualTo(expectedBody);
    }

    @Test
    void testCreateProjectInsufficientRole() {
        when(currentUser.get())
            .thenReturn(Mono.just(user));

        client.post().uri("/api/projects")
            .contentType(APPLICATION_JSON)
            .bodyValue(REQUEST_BODY)
            .exchange()
            .expectStatus().isForbidden()
            .expectBody(ApiError.class).isEqualTo(toApiError(HandlerException.atLeastRoleRequired(User.Role.ADMIN)));
    }

    @Test
    void testCreateProjectFailureInvalidUrl() {
        when(currentUser.get())
            .thenReturn(Mono.just(lamtev));

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
                .thenReturn(Mono.just(admin));

        // simulate github client error that is usually caused by wrong url
        when(githubClient.hasAdminPermission(anyString(), anyString()))
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
                .thenReturn(Mono.just(lamtev));

        // make repo existing
        when(projectRepo.notExists(anyLong()))
                .thenReturn(Mono.just(FALSE));

        when(githubClient.hasAdminPermission(anyString(), anyString()))
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
                .expectBody(ApiError.class).isEqualTo(toApiError(ProjectsHandlerException.alreadyExists(acculaAccula.identity())));
    }

    @Test
    void testCreateProjectFailureNoPermission() {
        when(currentUser.get())
                .thenReturn(Mono.just(lamtev));

        // disable admin permission
        when(githubClient.hasAdminPermission(anyString(), anyString()))
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
        when(githubUserRepo.upsert(any(GithubUser.class)))
                .thenReturn(Mono.just(acculaGithub));

        when(projectRepo.findById(anyLong()))
                .thenReturn(Mono.just(highload19Project));

        final var expectedBody = ModelToDtoConverter.convert(highload19Project);

        client.get().uri("/api/projects/{id}", highload19Project.id())
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
        when(projectRepo.getTop(anyInt()))
                .thenReturn(Flux.fromArray(new Project[]{highload19Project, highload19Project}));

        final var expectedBody = ModelToDtoConverter.convert(highload19Project);

        client.get().uri("/api/projects?count={count}", 2L)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectDto[].class).isEqualTo(new ProjectDto[]{expectedBody, expectedBody});
    }

    @Test
    void testDeleteProject() {
        when(currentUser.get(any()))
                .thenReturn(Mono.just(0L));

        when(projectRepo.hasAdmin(anyLong(), anyLong()))
            .thenReturn(Mono.just(TRUE));

        when(projectRepo.delete(anyLong(), anyLong()))
                .thenReturn(Mono.just(TRUE));

        client.delete().uri("/api/projects/{id}", highload19Project.id())
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void testGetConf() {
        when(currentUser.get(any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
                .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(anyLong()))
            .thenReturn(Mono.just(highload19Project));
        when(userRepo.findByGithubIds(anyCollection()))
            .thenReturn(Flux.fromIterable(List.of(user, user1)));
        when(pullRepo.findByProjectIdIncludingSecondaryRepos(anyLong()))
            .thenReturn(Flux.empty());
        final var adminIds = List.of(1L, 2L);
        final var expectedFiles = List.of(Project.Conf.KEEP_EXCLUDED_FILES_SYNCED, "file1", "file2", "file3");
        when(projectRepo.confById(anyLong()))
                .thenReturn(Mono.just(Project.Conf.defaultConf()
                    .withAdminIds(adminIds)
                    .withExcludedFiles(expectedFiles)
                    .withLanguages(List.of(CodeLanguage.JAVA))
                    .withExcludedSourceAuthorIds(List.of(1L))));

        when(githubClient.getRepoAdmins(anyString(), anyString()))
            .thenReturn(Mono.just(adminIds));

        when(projectService.headFiles(any()))
            .thenReturn(Mono.just(expectedFiles));

        when(projectRepo.supportedLanguages())
            .thenReturn(Mono.just(List.of(CodeLanguage.values())));

        client.get().uri("/api/projects/{id}/conf", highload19Project.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectConfDto.class)
                .isEqualTo(ProjectConfDto.builder()
                    .admins(new ValuesWithSuggestion<>(adminIds, Stream.of(user, user1).map(ModelToDtoConverter::convert).toList()))
                    .code(ProjectConfDto.Code.builder()
                        .fileMinSimilarityIndex(Project.Conf.defaultConf().fileMinSimilarityIndex())
                        .languages(new ValuesWithSuggestion<>(List.of(ProjectConfDto.Language.JAVA), List.of(ProjectConfDto.Language.values())))
                        .build())
                    .clones(ProjectConfDto.Clones.builder()
                        .minTokenCount(Project.Conf.defaultConf().cloneMinTokenCount())
                        .excludedFiles(new ValuesWithSuggestion<>(expectedFiles, expectedFiles))
                        .excludedSourceAuthors(new ValuesWithSuggestion<>(1L))
                        .build())
                    .build());
    }

    @Test
    void testGetConfForbidden() {
        mockForbidden();

        client.get().uri("/api/projects/{id}/conf", highload19Project.id())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void testGetConfNotFound() {
        mockNotFound();
        when(projectRepo.confById(anyLong()))
                .thenReturn(Mono.empty());
        when(pullRepo.findByProjectIdIncludingSecondaryRepos(anyLong()))
            .thenReturn(Flux.empty());

        client.get().uri("/api/projects/{id}/conf", highload19Project.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void testPutConf() {
        when(currentUser.get(any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
                .thenReturn(Mono.just(TRUE));
        final var adminIds = List.of(1L, 2L);
        when(projectRepo.upsertConf(anyLong(), any()))
                .thenReturn(Mono.just(Project.Conf.defaultConf().withAdminIds(adminIds)));

        client.put().uri("/api/projects/{id}/conf", highload19Project.id())
                .contentType(APPLICATION_JSON)
                .bodyValue(ProjectConfDto.builder()
                    .admins(new ValuesWithSuggestion<>(adminIds))
                    .code(ProjectConfDto.Code.builder()
                        .fileMinSimilarityIndex(Project.Conf.defaultConf().fileMinSimilarityIndex())
                        .languages(new ValuesWithSuggestion<>(ProjectConfDto.Language.JAVA))
                        .build())
                    .clones(ProjectConfDto.Clones.builder()
                        .minTokenCount(Project.Conf.defaultConf().cloneMinTokenCount())
                        .excludedFiles(new ValuesWithSuggestion<>(Project.Conf.defaultConf().excludedFiles()))
                        .excludedSourceAuthors(new ValuesWithSuggestion<>(1L))
                        .build())
                    .build())
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testPutConfBadRequest() {
        when(currentUser.get(any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
                .thenReturn(Mono.just(TRUE));

        client.put().uri("/api/projects/{id}/conf", highload19Project.id())
                .contentType(APPLICATION_JSON)
                .bodyValue(ProjectConfDto.builder().build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testRepoSuggestionOk() {
        when(currentUser.get(any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
            .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(anyLong()))
            .thenReturn(Mono.just(PROJECT_HIGHLOAD));
        when(githubClient.getAllRepos(anyInt()))
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
                .build()
        ).toArray(new RepoShortDto[0]);

        client.get().uri("/api/projects/{id}/repoSuggestion", PROJECT_HIGHLOAD.id())
            .exchange()
            .expectStatus().isOk()
            .expectBody(RepoShortDto[].class).isEqualTo(suggestion);
    }

    @Test
    void testRepoSuggestionForbidden() {
        when(currentUser.get(any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
            .thenReturn(Mono.just(FALSE));
        when(projectRepo.findById(anyLong()))
            .thenReturn(Mono.just(PROJECT_HIGHLOAD));
        when(githubClient.getAllRepos(anyInt()))
            .thenReturn(Flux.just(GH_REPO_HIGHLOAD2, GH_REPO, GH_REPO_HIGHLOAD3, GH_REPO_HIGHLOAD1));

        client.get().uri("/api/projects/{id}/repoSuggestion", PROJECT_HIGHLOAD.id())
            .exchange()
            .expectStatus().isForbidden()
            .expectBody(Void.class).value(Matchers.nullValue());
    }

    @Test
    void testAddRepoByUrlOk() {
        when(currentUser.get(any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
            .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(anyLong()))
            .thenReturn(Mono.just(PROJECT_HIGHLOAD));
        when(githubUserRepo.upsert(REPO_HIGHLOAD.owner()))
            .thenReturn(Mono.just(REPO_HIGHLOAD.owner()));
        when(repoRepo.upsert(REPO_HIGHLOAD))
            .thenReturn(Mono.just(REPO_HIGHLOAD));
        when(repoRepo.findByName(anyString(), anyString()))
            .thenReturn(Mono.just(REPO_HIGHLOAD));
        when(projectRepo.attachRepos(anyLong(), anyCollection()))
            .thenReturn(Mono.empty());
        when(githubClient.hasAdminPermission(anyString(), anyString()))
            .thenReturn(Mono.just(TRUE));
        when(githubClient.getRepo(anyString(), anyString()))
            .thenReturn(Mono.just(GH_REPO));
        when(githubClient.getRepositoryPulls(anyString(), anyString(), any(), anyInt()))
            .thenReturn(Flux.fromArray(OPEN_PULLS));
        when(projectRepo.updateState(anyLong(), any()))
            .thenReturn(Mono.empty());
        when(projectRepo.projectDoesNotContainRepo(anyLong(), anyLong()))
            .thenReturn(Mono.just(TRUE));
        when(projectService.init(anyList()))
            .thenReturn(Mono.empty());

        client.post().uri("/api/projects/{id}/addRepoByUrl", PROJECT_HIGHLOAD.id())
            .bodyValue(new AddRepoDto.ByUrl(REPO_URL_HIGHLOAD1))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Void.class).value(Matchers.nullValue());
    }

    @Test
    void testAddRepoForbidden() {
        when(currentUser.get(any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
            .thenReturn(Mono.just(FALSE));

        client.post().uri("/api/projects/{id}/addRepoByUrl", PROJECT_HIGHLOAD.id())
            .bodyValue(new AddRepoDto.ByUrl(REPO_URL_HIGHLOAD1))
            .exchange()
            .expectStatus().isForbidden()
            .expectBody(Void.class).value(Matchers.nullValue());
    }

    @Test
    void testAddRepoByInfoOk() {
        when(currentUser.get(any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
            .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(anyLong()))
            .thenReturn(Mono.just(PROJECT_HIGHLOAD));
        when(githubUserRepo.upsert(REPO_HIGHLOAD.owner()))
            .thenReturn(Mono.just(REPO_HIGHLOAD.owner()));
        when(repoRepo.upsert(REPO_HIGHLOAD))
            .thenReturn(Mono.just(REPO_HIGHLOAD));
        when(repoRepo.findByName(anyString(), anyString()))
            .thenReturn(Mono.empty());
        when(projectRepo.attachRepos(anyLong(), anyCollection()))
            .thenReturn(Mono.empty());
        when(githubClient.hasAdminPermission(anyString(), anyString()))
            .thenReturn(Mono.just(TRUE));
        when(githubClient.getRepo(anyString(), anyString()))
            .thenReturn(Mono.just(GH_REPO));
        when(githubClient.getRepositoryPulls(anyString(), anyString(), any(), anyInt()))
            .thenReturn(Flux.fromArray(OPEN_PULLS));
        when(projectRepo.updateState(anyLong(), any()))
            .thenReturn(Mono.empty());
        when(projectRepo.projectDoesNotContainRepo(anyLong(), anyLong()))
            .thenReturn(Mono.just(TRUE));
        when(projectService.init(anyList()))
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

    @Test
    void testAddRepoByInfoConflictAlreadyExists() {
        when(currentUser.get(any()))
            .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
            .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(anyLong()))
            .thenReturn(Mono.just(PROJECT_HIGHLOAD));
        when(repoRepo.findByName(anyString(), anyString()))
            .thenReturn(Mono.empty());
        when(githubClient.hasAdminPermission(anyString(), anyString()))
            .thenReturn(Mono.just(TRUE));
        when(githubClient.getRepo(anyString(), anyString()))
            .thenReturn(Mono.just(GH_REPO));
        when(githubClient.getRepositoryPulls(anyString(), anyString(), any(), anyInt()))
            .thenReturn(Flux.fromArray(OPEN_PULLS));
        when(projectRepo.updateState(anyLong(), any()))
            .thenReturn(Mono.empty());
        when(projectRepo.projectDoesNotContainRepo(anyLong(), anyLong()))
            .thenReturn(Mono.just(FALSE));

        var repoIdentity = GithubRepo.Identity.of(GH_REPO_HIGHLOAD1.owner().login(), GH_REPO_HIGHLOAD1.name());

        client.post().uri("/api/projects/{id}/addRepoByInfo", PROJECT_HIGHLOAD.id())
            .bodyValue(new AddRepoDto.ByInfo(RepoShortDto.builder()
                .id(GH_REPO_HIGHLOAD1.id())
                .owner(REPO_OWNER_HIGHLOAD)
                .name(REPO_NAME_HIGHLOAD1)
                .build()))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(ApiError.class).isEqualTo(toApiError(ProjectsHandlerException.alreadyExists(repoIdentity)));
    }

    private void mockNotFound() {
        when(currentUser.get(any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
                .thenReturn(Mono.just(TRUE));
        when(projectRepo.findById(anyLong()))
                .thenReturn(Mono.empty());
    }

    private void mockForbidden() {
        when(currentUser.get(any()))
                .thenReturn(Mono.just(0L));
        when(projectRepo.hasAdmin(anyLong(), anyLong()))
                .thenReturn(Mono.just(FALSE));
    }

    @SneakyThrows
    private static GithubClientException newGithubException() {
        final var ctor = GithubClientException.class.getDeclaredConstructor(Throwable.class);
        ctor.setAccessible(true);
        return ctor.newInstance(new Throwable());
    }
}
