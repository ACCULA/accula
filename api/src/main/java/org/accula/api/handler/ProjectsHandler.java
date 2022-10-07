package org.accula.api.handler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.config.WebhookProperties;
import org.accula.api.converter.DtoToModelConverter;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.converter.ModelToDtoConverter;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.UserRepo;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.api.GithubClientException;
import org.accula.api.github.model.GithubApiHook;
import org.accula.api.github.model.GithubApiPull.State;
import org.accula.api.handler.dto.AddRepoDto;
import org.accula.api.handler.dto.CreateProjectDto;
import org.accula.api.handler.dto.ProjectConfDto;
import org.accula.api.handler.dto.ProjectDto;
import org.accula.api.handler.dto.RepoShortDto;
import org.accula.api.handler.dto.validation.InputDtoValidator;
import org.accula.api.handler.exception.Http4xxException;
import org.accula.api.handler.exception.ProjectsHandlerException;
import org.accula.api.handler.exception.ResponseConvertibleException;
import org.accula.api.handler.suggestion.Suggester;
import org.accula.api.handler.util.RepoIdentityExtractor;
import org.accula.api.handler.util.Responses;
import org.accula.api.service.CloneDetectionService;
import org.accula.api.service.ProjectService;
import org.accula.api.util.Lambda;
import org.accula.api.util.ReactorOperators;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.context.ContextView;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static java.util.function.Predicate.isEqual;

/**
 * @author Anton Lamtev
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ProjectsHandler implements Handler {
    private final Suggester suggester = new Suggester();
    @Getter
    private final InputDtoValidator validator;
    private final WebhookProperties webhookProperties;
    @Getter
    private final CurrentUserRepo currentUserRepo;
    private final GithubClient githubClient;
    private final ProjectRepo projectRepo;
    private final PullRepo pullRepo;
    private final GithubRepoRepo repoRepo;
    private final GithubUserRepo githubUserRepo;
    private final UserRepo userRepo;
    private final ProjectService projectService;
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
                .switchIfEmpty(Mono.error(Http4xxException::notFound))
                .map(ModelToDtoConverter::convert)
                .flatMap(Responses::ok)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> create(final ServerRequest request) {
        return checkAdminRole()
                .then(request.bodyToMono(CreateProjectDto.class))
                .doOnNext(this::validate)
                .map(RepoIdentityExtractor::repoIdentity)
                .flatMap(this::retrieveGithubRepoInfo)
                .flatMap(this::saveProjectData)
                .flatMap(this::deleteExistingWebhook)
                .flatMap(this::createWebhook)
                .flatMap(Responses::created)
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> delete(final ServerRequest request) {
        return havingCreatorPermissionAtProject(request)
            .flatMap(this::deleteProject)
            .then(Responses.accepted())
            .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> getConf(final ServerRequest request) {
        return havingAdminPermissionAtProject(request)
            .flatMap(projectId -> Mono
                .zip(
                    projectRepo.confById(projectId),
                    githubAdmins(projectId),
                    projectRepo.supportedLanguages(),
                    headFiles(projectId),
                    allPullAuthors(projectId)
                )
                .subscribeOn(Schedulers.parallel())
                .map(TupleUtils.function(ModelToDtoConverter::convertConf))
            )
            .switchIfEmpty(Mono.error(Http4xxException::notFound))
            .flatMap(Responses::ok)
            .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> updateConf(final ServerRequest request) {
        return havingAdminPermissionAtProject(request)
                .zipWith(request.bodyToMono(ProjectConfDto.class)
                        .doOnNext(this::validate)
                        .map(DtoToModelConverter::convert))
                .flatMap(TupleUtils.function(projectRepo::upsertConf))
                .flatMap(Lambda.expandingWithArg(Responses::created))
                .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    public Mono<ServerResponse> addRepoByUrl(final ServerRequest request) {
        return addRepo(request, () -> request
            .bodyToMono(AddRepoDto.ByUrl.class)
            .doOnNext(this::validate)
            .map(RepoIdentityExtractor::repoIdentity));
    }

    public Mono<ServerResponse> addRepoByInfo(final ServerRequest request) {
        return addRepo(request, () -> request
            .bodyToMono(AddRepoDto.ByInfo.class)
            .doOnNext(this::validate)
            .map(RepoIdentityExtractor::repoIdentity));
    }

    public Mono<ServerResponse> repoSuggestion(final ServerRequest request) {
        return havingAdminPermissionAtProject(request)
            .flatMap(projectRepo::findById)
            .zipWith(githubClient.getAllRepos(GithubClient.MAX_PAGE_SIZE)
                .map(repo -> RepoShortDto.builder()
                    .id(repo.id())
                    .owner(repo.owner().login())
                    .name(repo.name())
                    .build())
                .collectList())
            .map(TupleUtils.function(this::suggestRepos))
            .flatMap(Responses::ok)
            .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    private Mono<GithubRepo> retrieveGithubRepoInfo(final GithubRepo.Identity identity) {
        final var owner = identity.owner();
        final var repo = identity.name();
        return Mono
            .zip(githubClient.hasAdminPermission(owner, repo).subscribeOn(Schedulers.parallel()),
                githubClient.getRepo(owner, repo).subscribeOn(Schedulers.parallel()))
            .onErrorMap(GithubClientException.class, e -> {
                log.warn("Github Api Client error:", e);
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
            .switchIfEmpty(Mono.error(ProjectsHandlerException.alreadyExists(repo.identity())))
            .flatMap(ok -> githubUserRepo.upsert(repo.owner())
                .doOnError(e -> log.error("Error saving github user: {}", repo.owner(), e)))
            .then(currentUserRepo.get())
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

        configuringProject(
            projectId,
            fetchReposMono
                .flatMap(projectService::init)
            )
            .flatMap(pulls -> cloneDetectionService.fillSuffixTree(projectId, Flux.fromIterable(pulls)))
            .contextWrite(context)
            .subscribe();
    }

    private <T> Mono<T> configuringProject(final Long projectId, final Mono<T> mono) {
        final var makeProjectConfigured = projectRepo.updateState(projectId, Project.State.CONFIGURED);
        return Mono.usingWhen(
            projectRepo.updateState(projectId, Project.State.CONFIGURING).then(mono),
            Mono::just,
            __ -> makeProjectConfigured,
            (__, e) -> {
                log.error("Error during project configuring", e);
                return makeProjectConfigured;
            },
            __ -> makeProjectConfigured
        );
    }

    private Mono<ProjectDto> createWebhook(final ProjectDto project) {
        final var hook = GithubApiHook.builder()
            .events(new GithubApiHook.Event[]{GithubApiHook.Event.PULL_REQUEST, GithubApiHook.Event.PUSH})
            .active(true)
            .config(GithubApiHook.Config.builder()
                .callbackUrl(webhookProperties.url())
                .secret(webhookProperties.secret())
                .insecure(webhookProperties.sslEnabled() ? GithubApiHook.Config.Insecurity.NO : GithubApiHook.Config.Insecurity.YES)
                .build())
            .build();
        return githubClient
                .createHook(project.repoOwner(), project.repoName(), hook)
                .thenReturn(project);
    }

    private Mono<ProjectDto> deleteExistingWebhook(final ProjectDto project) {
        final var owner = project.repoOwner();
        final var repo = project.repoName();
        return githubClient
            .listHooks(owner, repo)
            .flatMap(hooks -> Mono
                .justOrEmpty(hooks
                    .stream()
                    .filter(hook -> hook.config().callbackUrl().equals(webhookProperties.url()))
                    .findFirst()))
            .flatMap(hook -> githubClient.deleteHook(owner, repo, Objects.requireNonNull(hook.id())))
            .thenReturn(project);
    }

    private Mono<Void> deleteProject(final Project project) {
        return projectRepo
            .delete(project.id(), project.creator().id())
            .doOnNext(deletedSuccessfully -> {
                if (deletedSuccessfully) {
                    cloneDetectionService.dropSuffixTree(project.id());
                }
            })
            .then(deleteExistingWebhook(ModelToDtoConverter.convert(project)))
            .then();
    }

    private static Mono<Long> withProjectId(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::valueOf)
                .onErrorMap(NumberFormatException.class, Lambda.expandingWithArg(Http4xxException::badRequest));
    }

    private Mono<Long> havingAdminPermissionAtProject(final ServerRequest request) {
        return withProjectId(request)
            .filterWhen(this::isCurrentUserAdmin)
            .switchIfEmpty(Mono.error(Http4xxException::forbidden));
    }

    private Mono<Project> havingCreatorPermissionAtProject(final ServerRequest request) {
        return withProjectId(request)
            .flatMap(projectRepo::findById)
            .filterWhen(this::isCurrentUserCreator)
            .switchIfEmpty(Mono.error(Http4xxException::forbidden));
    }

    private Mono<Boolean> isCurrentUserAdmin(final Long projectId) {
        return currentUserRepo
                .get(User::id)
                .flatMap(currentUserId -> projectRepo.hasAdmin(projectId, currentUserId));
    }

    private Mono<Boolean> isCurrentUserCreator(final Project project) {
        return currentUserRepo
            .get(User::id)
            .map(currentUserId -> project.creator().id().equals(currentUserId));
    }

    private Mono<List<User>> githubAdmins(final Long projectId) {
        return projectRepo
            .findById(projectId)
            .flatMap(this::githubRepoAdmins)
            .flatMapMany(userRepo::findByGithubIds)
            .collectList()
            .onErrorResume(GithubClientException.class, e -> {
                log.warn("Cannot fetch repository admins", e);
                return Mono.just(List.of());
            });
    }

    private Mono<List<Long>> githubRepoAdmins(final Project project) {
        final var repo = project.githubRepo();
        return githubClient.getRepoAdmins(repo.owner().login(), repo.name());
    }

    private Mono<List<String>> headFiles(final Long projectId) {
        return projectRepo
            .findById(projectId)
            .map(Project::githubRepo)
            .flatMap(projectService::headFiles);
    }

    private Mono<List<GithubUser>> allPullAuthors(final Long projectId) {
        return pullRepo
            .findByProjectIdIncludingSecondaryRepos(projectId)
            .map(pull -> pull.head().repo().owner())
            .collect(Collectors.toSet())
            .map(users -> users.stream().sorted(Comparator.comparing(GithubUser::login)).toList());
    }

    private Mono<Project> saveDefaultConf(final Mono<Project> projectMono) {
        return Mono.usingWhen(
                projectMono,
                Mono::just,
                project -> projectRepo.upsertConf(project.id(), Project.Conf.defaultConf())
        );
    }

    private Mono<ServerResponse> addRepo(final ServerRequest request, final Supplier<Mono<GithubRepo.Identity>> repoIdSupplier) {
        return havingAdminPermissionAtProject(request)
            .flatMap(projectId -> repoIdSupplier.get()
                .flatMap(repoIdentity -> retrieveGithubRepoInfo(repoIdentity)
                    .filterWhen(repo -> projectRepo.projectDoesNotContainRepo(projectId, repo.id()))
                    .switchIfEmpty(Mono.error(ProjectsHandlerException.alreadyExists(repoIdentity))))
                .flatMap(repo -> githubUserRepo.upsert(repo.owner())
                    .then(repoRepo.upsert(repo)))
                .flatMap(repo -> projectRepo.attachRepos(projectId, List.of(repo.id()))
                    .thenReturn(repo))
                .doOnEach(ReactorOperators.onNextWithContext((repo, context) ->
                    fetchPullsAndFillSuffixTreeInBackground(projectId, repo, context)))
                .then(projectRepo.findById(projectId)))
            .map(ModelToDtoConverter::convert)
            .flatMap(Responses::ok)
            .onErrorResume(ResponseConvertibleException::onErrorResume);
    }

    private List<RepoShortDto> suggestRepos(final Project project, final List<RepoShortDto> reposUserHasAccessTo) {
        final var projectRepos = Stream.of(List.of(project.githubRepo()), project.secondaryRepos())
            .<GithubRepo>mapMulti(List::forEach)
            .map(GithubRepo::id)
            .collect(Collectors.toSet());
        return suggester
            .suggest(project.githubRepo().name(), reposUserHasAccessTo, RepoShortDto::name)
            .filter(repo -> !projectRepos.contains(repo.id()))
            .toList();
    }
}
