package org.accula.api.handler;

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
import org.accula.api.github.model.GithubApiPull.State;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.handler.dto.CreateProjectDto;
import org.accula.api.handler.dto.ProjectConfDto;
import org.accula.api.handler.dto.ProjectDto;
import org.accula.api.handler.dto.validation.Errors;
import org.accula.api.handler.dto.validation.InputDtoValidator;
import org.accula.api.handler.exception.CreateProjectException;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.Responses;
import org.accula.api.service.ProjectService;
import org.accula.api.util.Lambda;
import org.accula.api.util.ReactorPublishers;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.context.ContextView;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.List;

import static java.lang.Boolean.TRUE;
import static java.util.function.Predicate.isEqual;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ProjectsHandler {
    private static final String NOT_ENOUGH_PERMISSIONS = "Not enough permissions";
    private final Validator createProjectValidator = InputDtoValidator.forClass(CreateProjectDto.class);
    private final Validator confValidator = InputDtoValidator.forClass(ProjectConfDto.class);
    private final WebhookProperties webhookProperties;
    private final CurrentUserRepo currentUser;
    private final GithubClient githubClient;
    private final ProjectRepo projectRepo;
    private final GithubUserRepo githubUserRepo;
    private final UserRepo userRepo;
    private final ProjectService projectService;
    private final CodeLoader codeLoader;

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
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .map(ModelToDtoConverter::convert)
                .flatMap(Responses::ok)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> create(final ServerRequest request) {
        return request
                .bodyToMono(CreateProjectDto.class)
                .doOnNext(this::validate)
                .map(ProjectsHandler::extractOwnerAndRepo)
                .flatMap(TupleUtils.function(this::retrieveGithubInfoForProjectCreation))
                .onErrorMap(GithubClientException.class, e -> {
                    log.error("Github Api Client error:", e);
                    return CreateProjectException.wrongUrl();
                })
                .flatMap(TupleUtils.function(this::saveProjectData))
                .flatMap(this::createWebhook)
                .flatMap(Responses::created)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> delete(final ServerRequest request) {
        return withProjectId(request)
                .zipWith(currentUser.get(User::getId))
                .flatMap(TupleUtils.function(projectRepo::delete))
                .flatMap(Lambda.expandingWithArg(Responses::accepted))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> githubAdmins(final ServerRequest request) {
        final var adminsMono = withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(Http4xxException.forbidden(NOT_ENOUGH_PERMISSIONS)))
                .flatMap(projectRepo::findById)
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .flatMap(this::githubRepoAdmins)
                .flatMapMany(userRepo::findByGithubIds)
                .map(ModelToDtoConverter::convert)
                .collectList();
        return adminsMono
                .flatMap(Responses::ok)
                .onErrorResume(GithubClientException.class, e -> {
                    log.warn("Cannot fetch repository admins", e);
                    return Responses.badRequest();
                })
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> headFiles(final ServerRequest request) {
        return withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(Http4xxException.forbidden(NOT_ENOUGH_PERMISSIONS)))
                .flatMap(projectRepo::findById)
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .map(Project::getGithubRepo)
                .flatMapMany(codeLoader::loadFilenames)
                .collectList()
                .flatMap(Responses::ok)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> getConf(final ServerRequest request) {
        final var confMono = withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(Http4xxException.forbidden(NOT_ENOUGH_PERMISSIONS)))
                .flatMap(projectRepo::confById)
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .map(ModelToDtoConverter::convert);
        return confMono
                .flatMap(Responses::ok)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> updateConf(final ServerRequest request) {
        return withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(Http4xxException.forbidden(NOT_ENOUGH_PERMISSIONS)))
                .zipWith(request.bodyToMono(ProjectConfDto.class)
                        .doOnNext(this::validate)
                        .map(DtoToModelConverter::convert))
                .flatMap(TupleUtils.function(projectRepo::upsertConf))
                .flatMap(Lambda.expandingWithArg(Responses::created))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    private Mono<Tuple3<Boolean, GithubApiRepo, User>> retrieveGithubInfoForProjectCreation(final String owner, final String repo) {
        return Mono.zip(
                githubClient.hasAdminPermission(owner, repo).subscribeOn(Schedulers.parallel()),
                githubClient.getRepo(owner, repo).subscribeOn(Schedulers.parallel()),
                currentUser.get());
    }

    private Mono<ProjectDto> saveProjectData(final boolean isAdmin,
                                             final GithubApiRepo githubApiRepo,
                                             final User currentUser) {
        return Mono.defer(() -> {
            if (!isAdmin) {
                return Mono.error(CreateProjectException.noPermission());
            }

            final var projectGithubRepo = GithubApiToModelConverter.convert(githubApiRepo);

            return projectRepo
                    .notExists(projectGithubRepo.getId())
                    .filter(isEqual(TRUE))
                    .switchIfEmpty(Mono.error(CreateProjectException.alreadyExists()))
                    .flatMap(ok -> githubUserRepo.upsert(projectGithubRepo.getOwner())
                            .doOnError(e -> log.error("Error saving github user: {}", projectGithubRepo.getOwner(), e)))
                    .flatMap(repoOwner -> projectRepo.upsert(projectGithubRepo, currentUser)
                            .doOnError(e -> log.error("Error saving Project: {}-{}", projectGithubRepo.getOwner(), currentUser, e)))
                    .transform(this::saveDefaultConf)
                    .map(ModelToDtoConverter::convert)
                    .doOnEach(ReactorPublishers.onNextWithContext(this::fetchPullsInBackground));
        });
    }

    private void fetchPullsInBackground(final ProjectDto project, final ContextView context) {
        final var repoOwner = project.getRepoOwner();
        final var repoName = project.getRepoName();
        githubClient.getRepositoryPulls(repoOwner, repoName, State.ALL, 100)
                .collectList()
                .flatMap(pulls -> projectService.update(project.getId(), pulls).collectList())
                .then(projectRepo.updateState(project.getId(), Project.State.CREATED))
                .contextWrite(context)
                .subscribe();
    }

    private Mono<ProjectDto> createWebhook(final ProjectDto project) {
        final var hook = GithubApiHook.onPullUpdates(webhookProperties.getUrl(), webhookProperties.getSecret());
        return githubClient
                .createHook(project.getRepoOwner(), project.getRepoName(), hook)
                .thenReturn(project);
    }

    private static Tuple2<String, String> extractOwnerAndRepo(final CreateProjectDto requestBody) {
        final var pathSegments = UriComponentsBuilder
                .fromUriString(requestBody.getGithubRepoUrl())
                .build()
                .getPathSegments();
        if (pathSegments.size() != 2) {
            throw CreateProjectException.invalidUrl();
        }

        return Tuples.of(pathSegments.get(0), pathSegments.get(1));
    }

    private static Mono<Long> withProjectId(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::parseLong)
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest));
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

    private void validate(final CreateProjectDto createProjectDto) {
        final var errors = new Errors(createProjectDto, "createProjectDto");
        createProjectValidator.validate(createProjectDto, errors);
        if (errors.hasErrors()) {
            throw CreateProjectException.badFormat(errors.joinedDescription());
        }
    }

    private void validate(final ProjectConfDto confDto) {
        final var errors = new Errors(confDto, "confDto");
        confValidator.validate(confDto, errors);
        if (errors.hasErrors()) {
            throw Http4xxException.badRequest("Bad format: %s".formatted(errors.joinedDescription()));
        }
    }
}
