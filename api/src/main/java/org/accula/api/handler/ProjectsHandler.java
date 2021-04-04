package org.accula.api.handler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.CodeLoader;
import org.accula.api.config.WebhookProperties;
import org.accula.api.converter.DtoToModelConverter;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.api.GithubClientException;
import org.accula.api.github.model.GithubApiHook;
import org.accula.api.github.model.GithubApiPull.State;
import org.accula.api.handler.dto.AttachRepoDto;
import org.accula.api.handler.dto.CreateProjectDto;
import org.accula.api.handler.dto.InputDto;
import org.accula.api.handler.dto.ProjectConfDto;
import org.accula.api.handler.dto.ProjectDto;
import org.accula.api.handler.dto.RepoShortDto;
import org.accula.api.handler.dto.validation.Errors;
import org.accula.api.handler.dto.validation.InputDtoValidator;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ProjectsHandlerException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.util.Responses;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.service.ProjectService;
import org.accula.api.util.Lambda;
import org.accula.api.util.ReactorOperators;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.context.ContextView;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;
import static java.util.function.Predicate.isEqual;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ProjectsHandler {
    private final Cache<Long, GithubRepo> repoSuggestionCache = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1L))
        .build();
    private final Suggester suggester = new Suggester();
    private final InputDtoValidator validator;
    private final WebhookProperties webhookProperties;
    private final CurrentUserRepo currentUser;
    private final GithubClient githubClient;
    private final ProjectRepo projectRepo;
    private final GithubRepoRepo repoRepo;
    private final GithubUserRepo githubUserRepo;
    private final UserRepo userRepo;
    private final ProjectService projectService;
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
                .flatMap(TupleUtils.function(this::retrieveGithubRepoInfo))
                .flatMap(this::saveProjectData)
                .flatMap(this::createWebhook)
                .flatMap(Responses::created)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> delete(final ServerRequest request) {
        return withProjectId(request)
                .zipWith(currentUser.get(User::id))
                .flatMap(TupleUtils.function(projectRepo::delete))
                .flatMap(Lambda.expandingWithArg(Responses::accepted))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> githubAdmins(final ServerRequest request) {
        final var adminsMono = withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(Http4xxException.forbidden()))
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
                .switchIfEmpty(Mono.error(Http4xxException.forbidden()))
                .flatMap(projectRepo::findById)
                .switchIfEmpty(Mono.error(Http4xxException.notFound()))
                .map(Project::githubRepo)
                .flatMapMany(codeLoader::loadFilenames)
                .collectList()
                .flatMap(Responses::ok)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> getConf(final ServerRequest request) {
        final var confMono = withProjectId(request)
                .filterWhen(this::isCurrentUserAdmin)
                .switchIfEmpty(Mono.error(Http4xxException.forbidden()))
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
                .switchIfEmpty(Mono.error(Http4xxException.forbidden()))
                .zipWith(request.bodyToMono(ProjectConfDto.class)
                        .doOnNext(this::validate)
                        .map(DtoToModelConverter::convert))
                .flatMap(TupleUtils.function(projectRepo::upsertConf))
                .flatMap(Lambda.expandingWithArg(Responses::created))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> attachRepoByUrl(final ServerRequest request) {
        return attachRepo(request, () -> request
            .bodyToMono(AttachRepoDto.ByUrl.class)
            .doOnNext(this::validate)
            .map(ProjectsHandler::extractOwnerAndRepo)
            .flatMap(ownerAndName -> repoRepo
                .findByName(ownerAndName.getT1(), ownerAndName.getT2())
                .switchIfEmpty(retrieveGithubRepoInfo(ownerAndName.getT1(), ownerAndName.getT2()))));
    }

    public Mono<ServerResponse> attachRepoByInfo(final ServerRequest request) {
        return attachRepo(request, () -> request
            .bodyToMono(AttachRepoDto.ByInfo.class)
            .doOnNext(this::validate)
            .map(AttachRepoDto.ByInfo::info)
            .flatMap(repoInfo -> Mono
                .justOrEmpty(repoSuggestionCache.getIfPresent(repoInfo.id()))
                .switchIfEmpty(retrieveGithubRepoInfo(repoInfo.owner(), repoInfo.name()))));
    }

    public Mono<ServerResponse> repoSuggestion(final ServerRequest request) {
        return withProjectId(request)
            .flatMap(projectRepo::findById)
            .map(Project::githubRepo)
            .zipWith(githubClient.getAllRepos(GithubClient.MAX_PAGE_SIZE)
                .map(GithubApiToModelConverter::convert)
                .doOnNext(repo -> repoSuggestionCache.put(repo.id(), repo))
                .map(repo -> RepoShortDto.builder()
                    .id(repo.id())
                    .owner(repo.owner().login())
                    .name(repo.name())
                    .build())
                .collectList())
            .map(TupleUtils.function(this::suggestRepos))
            .flatMap(Responses::ok);
    }

    private Mono<GithubRepo> retrieveGithubRepoInfo(final String owner, final String repo) {
        return Mono
            .zip(githubClient.hasAdminPermission(owner, repo).subscribeOn(Schedulers.parallel()),
                githubClient.getRepo(owner, repo).subscribeOn(Schedulers.parallel()))
            .onErrorMap(GithubClientException.class, e -> {
                log.error("Github Api Client error:", e);
                return ProjectsHandlerException.unableRetrieveGithubRepo(owner, repo);
            })
            .handle((hasAdminPermissionAndRepo, sink) -> {
                final var hasAdminPermission = hasAdminPermissionAndRepo.getT1();
                if (!hasAdminPermission) {
                    sink.error(ProjectsHandlerException.noPermission());
                    return;
                }
                final var githubApiRepo = hasAdminPermissionAndRepo.getT2();
                sink.next(GithubApiToModelConverter.convert(githubApiRepo));
            });
    }

    private Mono<ProjectDto> saveProjectData(final GithubRepo repo) {
        return projectRepo
            .notExists(repo.id())
            .filter(isEqual(TRUE))
            .switchIfEmpty(Mono.error(ProjectsHandlerException.alreadyExists(repo)))
            .flatMap(ok -> githubUserRepo.upsert(repo.owner())
                .doOnError(e -> log.error("Error saving github user: {}", repo.owner(), e)))
            .then(currentUser.get())
            .flatMap(currentUser -> projectRepo.upsert(repo, currentUser)
                .doOnError(e -> log.error("Error saving Project: {}-{}", repo.owner(), currentUser, e)))
            .transform(this::saveDefaultConf)
            .doOnEach(ReactorOperators.onNextWithContext((project, context) ->
                fetchPullsAndFillSuffixTreeInBackground(project.id(), project.githubRepo(), context)))
            .map(ModelToDtoConverter::convert);
    }

    private void fetchPullsAndFillSuffixTreeInBackground(final Long projectId, final GithubRepo repo, final ContextView context) {
        final var repoOwner = repo.owner().login();
        final var repoName = repo.name();
        final var fetchReposMono = githubClient
            .getRepositoryPulls(repoOwner, repoName, State.ALL, GithubClient.MAX_PAGE_SIZE)
            .collectList();
        projectRepo.updateState(projectId, Project.State.CONFIGURING)
            .then(fetchReposMono)
            .flatMap(projectService::init)
            .flatMap(pulls -> projectRepo.updateState(projectId, Project.State.CONFIGURED)
                .then(cloneDetectionService.fillSuffixTree(projectId, Flux.fromIterable(pulls))))
            .contextWrite(context)
            .subscribe();
    }

    private Mono<ProjectDto> createWebhook(final ProjectDto project) {
        final var hook = GithubApiHook.onPullUpdates(webhookProperties.url(), webhookProperties.secret());
        return githubClient
                .createHook(project.repoOwner(), project.repoName(), hook)
                .thenReturn(project);
    }

    private static Tuple2<String, String> extractOwnerAndRepo(final CreateProjectDto requestBody) {
        return extractOwnerAndRepo(requestBody.githubRepoUrl());
    }

    private static Tuple2<String, String> extractOwnerAndRepo(final AttachRepoDto.ByUrl requestBody) {
        return extractOwnerAndRepo(requestBody.url());
    }

    private static Tuple2<String, String> extractOwnerAndRepo(final String url) {
        final var pathSegments = UriComponentsBuilder
                .fromUriString(url)
                .build()
                .getPathSegments();
        if (pathSegments.size() != 2) {
            throw ProjectsHandlerException.invalidUrl(url);
        }
        return Tuples.of(pathSegments.get(0), pathSegments.get(1));
    }

    private static Mono<Long> withProjectId(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::valueOf)
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest));
    }

    private Mono<Boolean> isCurrentUserAdmin(final Long projectId) {
        return currentUser
                .get(User::id)
                .flatMap(currentUserId -> projectRepo.hasAdmin(projectId, currentUserId));
    }

    private Mono<List<Long>> githubRepoAdmins(final Project project) {
        final var repo = project.githubRepo();
        return githubClient.getRepoAdmins(repo.owner().login(), repo.name());
    }

    private Mono<Project> saveDefaultConf(final Mono<Project> projectMono) {
        return Mono.usingWhen(
                projectMono,
                Mono::just,
                project -> projectRepo.upsertConf(project.id(), Project.Conf.DEFAULT)
        );
    }

    private Mono<ServerResponse> attachRepo(final ServerRequest request, final Supplier<Mono<GithubRepo>> repoSupplier) {
        return withProjectId(request)
            .filterWhen(this::isCurrentUserAdmin)
            .switchIfEmpty(Mono.error(Http4xxException.forbidden()))
            .flatMap(projectId -> repoSupplier.get()
                .flatMap(repoRepo::upsert)
                .flatMap(repo -> projectRepo.attachRepos(projectId, List.of(repo.id()))
                    .thenReturn(repo))
                .doOnEach(ReactorOperators.onNextWithContext((repo, context) ->
                    fetchPullsAndFillSuffixTreeInBackground(projectId, repo, context))))
            .flatMap(Lambda.expandingWithArg(Responses::ok))
            .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    private List<RepoShortDto> suggestRepos(final GithubRepo mainRepo, final List<RepoShortDto> reposUserHasAccessTo) {
        return suggester.suggest(mainRepo.name(), reposUserHasAccessTo, RepoShortDto::name);
    }

    private void validate(final InputDto dto) {
        validate(dto, ProjectsHandlerException::badFormat, "Bad format: %s");
    }

    private void validate(final InputDto object,
                          final Function<String, ResponseConvertibleException> exceptionFactory,
                          final String... exceptionMessageFormat) {
        final var errors = new Errors(object, object.getClass().getSimpleName());
        validator.validate(object, errors);
        if (errors.hasErrors()) {
            final var format = exceptionMessageFormat.length == 1 ? exceptionMessageFormat[0] : "%s";
            throw exceptionFactory.apply(format.formatted(errors.joinedDescription()));
        }
    }
}
