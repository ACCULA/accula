package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.CodeLoader;
import org.accula.api.config.WebhookProperties;
import org.accula.api.converter.DtoToModelConverter;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.api.GithubClientException;
import org.accula.api.github.model.GithubApiHook;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiPull.State;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.handlers.dto.ProjectConfDto;
import org.accula.api.handlers.dto.ProjectDto;
import org.accula.api.handlers.dto.UserDto;
import org.accula.api.handlers.request.CreateProjectRequestBody;
import org.accula.api.handlers.response.ErrorBody;
import org.accula.api.handlers.util.ProjectUpdater;
import org.accula.api.util.ReactorSchedulers;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.List;

import static java.util.function.Predicate.isEqual;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ProjectsHandler {
    private static final Exception PROJECT_NOT_FOUND_EXCEPTION = new Exception();
    private static final String MASTER_BRANCH = "master"; // TODO: move it to project conf one day

    private final Scheduler remoteCallsScheduler = ReactorSchedulers.boundedElastic(this);
    private final WebhookProperties webhookProperties;
    private final CurrentUserRepo currentUser;
    private final GithubClient githubClient;
    private final ProjectRepo projectRepo;
    private final GithubUserRepo githubUserRepo;
    private final UserRepo userRepo;
    private final ProjectUpdater projectUpdater;
    private final GithubApiToModelConverter githubToModelConverter;
    private final CodeLoader codeLoader;

    public Mono<ServerResponse> getTop(final ServerRequest request) {
        return Mono
                .just(request.queryParam("count").orElse("5"))
                .map(Integer::parseInt)
                .flatMap(count -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .body(projectRepo.getTop(count).map(ModelToDtoConverter::convert), ProjectDto.class))
                .doOnSuccess(response -> log.debug("{}: {}", request, response.statusCode()));
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return withProjectId(request)
                .flatMap(projectRepo::findById)
                .map(ModelToDtoConverter::convert)
                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
                .flatMap(project -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(project))
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, ProjectsHandler::notFound)
                .doOnSuccess(response -> log.debug("{}: {}", request, response.statusCode()));
    }

    public Mono<ServerResponse> create(final ServerRequest request) {
        return request
                .bodyToMono(CreateProjectRequestBody.class)
                .onErrorResume(e -> Mono.error(CreateProjectException.BAD_FORMAT))
                .map(ProjectsHandler::extractOwnerAndRepo)
                .flatMap(TupleUtils.function(this::retrieveGithubInfoForProjectCreation))
                .onErrorMap(GithubClientException.class, e -> {
                    log.error("Github Api Client error:", e);
                    return CreateProjectException.WRONG_URL;
                })
                .flatMap(TupleUtils.function(this::saveProjectData))
                .flatMap(this::createWebhook)
                .flatMap(project -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(project))
                .doOnNext(response -> log.debug("{}: {}", request, response.statusCode()))
                .doOnError(e -> log.error("{}: ", request, e))
                .onErrorResume(CreateProjectException.class,
                        e -> switch (CreateProjectException.error(e)) {
                            case BAD_FORMAT, INVALID_URL, WRONG_URL, ALREADY_EXISTS -> ServerResponse
                                    .badRequest()
                                    .bodyValue(CreateProjectException.errorBody(e));
                            case NO_PERMISSION -> ServerResponse
                                    .status(FORBIDDEN)
                                    .bodyValue(CreateProjectException.errorBody(e));
                        });
    }

    public Mono<ServerResponse> delete(final ServerRequest request) {
        return withProjectId(request)
                .zipWith(currentUser.get(User::getId))
                .flatMap(TupleUtils.function(projectRepo::delete))
                .flatMap(success -> ServerResponse.ok().build())
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, ProjectsHandler::notFound);
    }

    public Mono<ServerResponse> githubAdmins(final ServerRequest request) {
        final var admins = withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                //TODO: not enough rights
                .flatMap(projectRepo::findById)
                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
                .flatMap(this::githubRepoAdmins)
                .flatMapMany(userRepo::findByGithubIds)
                .map(ModelToDtoConverter::convert);
        return ServerResponse
                .ok()
                .contentType(APPLICATION_JSON)
                .body(admins, UserDto.class)
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, ProjectsHandler::notFound)
                .onErrorResume(GithubClientException.class, e -> {
                    log.warn("Cannot fetch repository admins", e);
                    return ServerResponse.badRequest().build();
                });
    }

    public Mono<ServerResponse> baseFiles(ServerRequest request) {
        final var files = withProjectId(request)
                .flatMap(projectRepo::findById)
                .map(project -> CommitSnapshot.builder()
                        .branch(MASTER_BRANCH)
                        .repo(project.getGithubRepo())
                        .build())
                .flatMapMany(codeLoader::loadFilenames)
                .collectList();
        return ServerResponse
                .ok()
                .contentType(APPLICATION_JSON)
                .body(files, List.class);
    }

    public Mono<ServerResponse> getConf(final ServerRequest request) {
        final var conf = withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                //TODO: not enough rights
                .flatMap(projectRepo::confById)
                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
                .map(ModelToDtoConverter::convert);
        return ServerResponse
                .ok()
                .contentType(APPLICATION_JSON)
                .body(conf, ProjectConfDto.class)
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, ProjectsHandler::notFound);
    }

    public Mono<ServerResponse> updateConf(final ServerRequest request) {
        return withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                //TODO: not enough rights
                .zipWith(request.bodyToMono(ProjectConfDto.class)
                        .map(DtoToModelConverter::convert))
                .flatMap(TupleUtils.function(projectRepo::upsertConf))
                .flatMap(conf -> ServerResponse.ok().build())
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, ProjectsHandler::notFound)
                .onErrorResume(DtoToModelConverter.ValidationException.class, ProjectsHandler::badRequest);
    }

    private Mono<Tuple4<Boolean, GithubApiRepo, GithubApiPull[], User>> retrieveGithubInfoForProjectCreation(final String owner,
                                                                                                             final String repo) {
        //@formatter:off
        return Mono.zip(githubClient.hasAdminPermission(owner, repo).subscribeOn(remoteCallsScheduler),
                        githubClient.getRepo(owner, repo).subscribeOn(remoteCallsScheduler),
                        githubClient.getRepositoryPulls(owner, repo, State.ALL).subscribeOn(remoteCallsScheduler),
                        currentUser.get());
        //@formatter:on
    }

    private Mono<ProjectDto> saveProjectData(final boolean isAdmin,
                                             final GithubApiRepo githubApiRepo,
                                             final GithubApiPull[] githubApiPulls,
                                             final User currentUser) {
        return Mono.defer(() -> {
            if (!isAdmin) {
                return Mono.error(CreateProjectException.NO_PERMISSION);
            }

            final var projectGithubRepo = githubToModelConverter.convert(githubApiRepo);

            return projectRepo
                    .notExists(projectGithubRepo.getId())
                    .filter(isEqual(true))
                    .switchIfEmpty(Mono.error(CreateProjectException.ALREADY_EXISTS))
                    .flatMap(ok -> githubUserRepo.upsert(projectGithubRepo.getOwner())
                            .doOnError(e -> log.error("Error saving github user: {}", projectGithubRepo.getOwner(), e)))
                    .flatMap(repoOwner -> projectRepo.upsert(projectGithubRepo, currentUser)
                            .doOnError(e -> log.error("Error saving Project: {}-{}", projectGithubRepo.getOwner(), currentUser, e)))
                    .transform(this::saveDefaultConf)
                    .flatMap(project -> projectUpdater.update(project.getId(), githubApiPulls)
                            .map(openPullCount -> ModelToDtoConverter.convert(project, openPullCount)));
        });
    }

    private Mono<ProjectDto> createWebhook(final ProjectDto project) {
        final var hook = GithubApiHook.onPullUpdates(webhookProperties.getUrl(), webhookProperties.getSecret());
        return githubClient
                .createHook(project.getRepoOwner(), project.getRepoName(), hook)
                .thenReturn(project);
    }

    private static Tuple2<String, String> extractOwnerAndRepo(final CreateProjectRequestBody requestBody) {
        var url = requestBody.getGithubRepoUrl();

        if (url.startsWith("https://")) {
            url = url.replace("https://", "");
        }
        if (url.startsWith("github.com")) {
            url = url.replace("github.com", "");
        } else {
            url = null;
        }

        if (url == null) {
            throw CreateProjectException.INVALID_URL;
        }

        final var pathComponents = Arrays.stream(url.split("/"))
                .filter(not(String::isBlank))
                .collect(toList());

        if (pathComponents.size() != 2) {
            throw CreateProjectException.WRONG_URL;
        }

        return Tuples.of(pathComponents.get(0), pathComponents.get(1));
    }

    private static Mono<Long> withProjectId(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::parseLong)
                .onErrorMap(NumberFormatException.class, e -> PROJECT_NOT_FOUND_EXCEPTION);
    }

    private Mono<Boolean> isCurrentUserAdmin(final Long projectId) {
        return currentUser
                .get(User::getId)
                .flatMap(currentUserId -> projectRepo.hasAdmin(projectId, currentUserId));
    }

    private Mono<List<Long>> githubRepoAdmins(final Project project) {
        final var repo = project.getGithubRepo();
        return githubClient.getRepoAdmins(repo.getOwner().getLogin(), repo.getName());
    }

    private Mono<Project> saveDefaultConf(final Mono<Project> projectMono) {
        return Mono.usingWhen(
                projectMono,
                Mono::just,
                project -> projectRepo.upsertConf(project.getId(), Project.Conf.DEFAULT)
        );
    }

    private static Mono<ServerResponse> notFound(final Throwable error) {
        return ServerResponse.notFound().build();
    }

    private static Mono<ServerResponse> badRequest(final Throwable error) {
        return ServerResponse.badRequest().build();
    }

    @RequiredArgsConstructor
    private static final class CreateProjectException extends RuntimeException {
        private static final long serialVersionUID = 2418056639476069599L;

        private static final CreateProjectException BAD_FORMAT = new CreateProjectException(Error.BAD_FORMAT);
        private static final CreateProjectException INVALID_URL = new CreateProjectException(Error.INVALID_URL);
        private static final CreateProjectException ALREADY_EXISTS = new CreateProjectException(Error.ALREADY_EXISTS);
        private static final CreateProjectException WRONG_URL = new CreateProjectException(Error.WRONG_URL);
        private static final CreateProjectException NO_PERMISSION = new CreateProjectException(Error.NO_PERMISSION);

        private final Error error;

        private static Error error(final Throwable t) {
            return ((CreateProjectException) t).error;
        }

        private static ErrorBody errorBody(final Throwable t) {
            return new ErrorBody(error(t).toString());
        }

        private enum Error {
            BAD_FORMAT,
            INVALID_URL,
            ALREADY_EXISTS,
            WRONG_URL,
            NO_PERMISSION,
        }
    }
}
