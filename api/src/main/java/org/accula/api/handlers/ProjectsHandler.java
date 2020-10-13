package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.CodeLoader;
import org.accula.api.config.WebhookProperties;
import org.accula.api.converter.DtoToModelConverter;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.converter.ModelToDtoConverter;
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
import org.accula.api.handlers.request.CreateProjectRequestBody;
import org.accula.api.handlers.response.ErrorBody;
import org.accula.api.handlers.util.ProjectUpdater;
import org.accula.api.handlers.util.Responses;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.util.Lambda;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.List;

import static java.lang.Boolean.TRUE;
import static java.util.function.Predicate.isEqual;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ProjectsHandler {
    //TODO: common handler for all NOT FOUND cases
    private static final Exception PROJECT_NOT_FOUND_EXCEPTION = new Exception("PROJECT_NOT_FOUND_EXCEPTION");
    private static final Exception NOT_ENOUGH_PERMISSIONS_EXCEPTION = new Exception("NOT_ENOUGH_PERMISSIONS_EXCEPTION");

    private final WebhookProperties webhookProperties;
    private final CurrentUserRepo currentUser;
    private final GithubClient githubClient;
    private final ProjectRepo projectRepo;
    private final GithubUserRepo githubUserRepo;
    private final UserRepo userRepo;
    private final ProjectUpdater projectUpdater;
    private final CodeLoader codeLoader;
    private final CloneDetectionService cloneDetectionService;

    public Mono<ServerResponse> getTop(final ServerRequest request) {
        return Mono
                .just(request.queryParam("count").orElse("5"))
                .map(Integer::parseInt)
                .flatMap(count -> projectRepo
                        .getTop(count)
                        .map(ModelToDtoConverter::convert)
                        .collectList())
                .flatMap(Responses::ok);
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return withProjectId(request)
                .flatMap(projectRepo::findById)
                .map(ModelToDtoConverter::convert)
                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
                .flatMap(Responses::ok)
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, Lambda.expandingWithArg(Responses::notFound));
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
                .flatMap(this::detectClones)
                .flatMap(this::createWebhook)
                .flatMap(Responses::created)
                .doOnError(e -> log.error("{}: ", request, e))
                .onErrorResume(CreateProjectException.class, e ->
                        switch (CreateProjectException.error(e)) {
                            case BAD_FORMAT, INVALID_URL, WRONG_URL, ALREADY_EXISTS -> Responses
                                    .badRequest(CreateProjectException.errorBody(e));
                            case NO_PERMISSION -> Responses.forbidden(CreateProjectException.errorBody(e));
                        });
    }

    public Mono<ServerResponse> delete(final ServerRequest request) {
        return withProjectId(request)
                .zipWith(currentUser.get(User::getId))
                .flatMap(TupleUtils.function(projectRepo::delete))
                .flatMap(Lambda.expandingWithArg(Responses::accepted))
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, Lambda.expandingWithArg(Responses::notFound));
    }

    public Mono<ServerResponse> githubAdmins(final ServerRequest request) {
        final var adminsMono = withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(NOT_ENOUGH_PERMISSIONS_EXCEPTION))
                .flatMap(projectRepo::findById)
                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
                .flatMap(this::githubRepoAdmins)
                .flatMapMany(userRepo::findByGithubIds)
                .map(ModelToDtoConverter::convert)
                .collectList();
        return adminsMono
                .flatMap(Responses::ok)
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, Lambda.expandingWithArg(Responses::notFound))
                .onErrorResume(NOT_ENOUGH_PERMISSIONS_EXCEPTION::equals, Lambda.expandingWithArg(Responses::forbidden))
                .onErrorResume(GithubClientException.class, e -> {
                    log.warn("Cannot fetch repository admins", e);
                    return Responses.badRequest();
                });
    }

    public Mono<ServerResponse> headFiles(ServerRequest request) {
        return withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(NOT_ENOUGH_PERMISSIONS_EXCEPTION))
                .flatMap(projectRepo::findById)
                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
                .map(Project::getGithubRepo)
                .flatMapMany(codeLoader::loadFilenames)
                .collectList()
                .flatMap(Responses::ok)
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, Lambda.expandingWithArg(Responses::notFound))
                .onErrorResume(NOT_ENOUGH_PERMISSIONS_EXCEPTION::equals, Lambda.expandingWithArg(Responses::forbidden));
    }

    public Mono<ServerResponse> getConf(final ServerRequest request) {
        final var confMono = withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(NOT_ENOUGH_PERMISSIONS_EXCEPTION))
                .flatMap(projectRepo::confById)
                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
                .map(ModelToDtoConverter::convert);
        return confMono
                .flatMap(Responses::ok)
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, Lambda.expandingWithArg(Responses::notFound))
                .onErrorResume(NOT_ENOUGH_PERMISSIONS_EXCEPTION::equals, Lambda.expandingWithArg(Responses::forbidden));
    }

    public Mono<ServerResponse> updateConf(final ServerRequest request) {
        return withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(NOT_ENOUGH_PERMISSIONS_EXCEPTION))
                .zipWith(request.bodyToMono(ProjectConfDto.class)
                        .map(DtoToModelConverter::convert))
                .flatMap(TupleUtils.function(projectRepo::upsertConf))
                .flatMap(Lambda.expandingWithArg(Responses::created))
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, Lambda.expandingWithArg(Responses::notFound))
                .onErrorResume(NOT_ENOUGH_PERMISSIONS_EXCEPTION::equals, Lambda.expandingWithArg(Responses::forbidden))
                .onErrorResume(DtoToModelConverter.ValidationException.class, Lambda.expandingWithArg(Responses::badRequest));
    }

    private Mono<Tuple4<Boolean, GithubApiRepo, List<GithubApiPull>, User>> retrieveGithubInfoForProjectCreation(final String owner,
                                                                                                                 final String repo) {
        return Mono.zip(
                githubClient.hasAdminPermission(owner, repo).subscribeOn(Schedulers.boundedElastic()),
                githubClient.getRepo(owner, repo).subscribeOn(Schedulers.boundedElastic()),
                githubClient.getRepositoryPulls(owner, repo, State.ALL, 100).collectList().subscribeOn(Schedulers.boundedElastic()),
                currentUser.get());
    }

    private Mono<ProjectDto> saveProjectData(final boolean isAdmin,
                                             final GithubApiRepo githubApiRepo,
                                             final List<GithubApiPull> githubApiPulls,
                                             final User currentUser) {
        return Mono.defer(() -> {
            if (!isAdmin) {
                return Mono.error(CreateProjectException.NO_PERMISSION);
            }

            final var projectGithubRepo = GithubApiToModelConverter.convert(githubApiRepo);

            return projectRepo
                    .notExists(projectGithubRepo.getId())
                    .filter(isEqual(TRUE))
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

    private Mono<ProjectDto> detectClones(final ProjectDto project) {
        return cloneDetectionService
                .detectClones(project.getId())
                .then(Mono.just(project));
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
