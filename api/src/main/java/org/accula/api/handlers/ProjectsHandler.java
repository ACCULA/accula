package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.config.WebhookProperties;
import org.accula.api.converter.DataConverter;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.User;
import org.accula.api.db.repo.CommitRepo;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.api.GithubClientException;
import org.accula.api.github.model.GithubApiHook;
import org.accula.api.github.model.GithubApiPull;
import org.accula.api.github.model.GithubApiPull.State;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.handlers.dto.ProjectDto;
import org.accula.api.handlers.request.CreateProjectRequestBody;
import org.accula.api.handlers.response.ErrorBody;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    private final WebhookProperties webhookProperties;
    private final CurrentUserRepo currentUser;
    private final GithubClient githubClient;
    private final ProjectRepo projectRepo;
    private final GithubUserRepo githubUserRepo;
    private final GithubRepoRepo githubRepoRepo;
    private final CommitRepo commitRepo;
    private final PullRepo pullRepo;
    private final DataConverter converter;
    private final Scheduler remoteCallsScheduler = Schedulers.boundedElastic();
    private final Scheduler pullProcessingScheduler = Schedulers.boundedElastic();

    //FIXME: actual open pull count
    public Mono<ServerResponse> getTop(final ServerRequest request) {
        return Mono
                .just(request.queryParam("count").orElse("5"))
                .map(Integer::parseInt)
                .flatMap(count -> ServerResponse
                        .ok()
                        .body(projectRepo
                                .getTop(count)
                                .map(project -> converter.convert(project, 0)), ProjectDto.class))
                .doOnSuccess(response -> log.debug("{}: {}", request, response.statusCode()));
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::parseLong)
                .onErrorMap(e -> e instanceof NumberFormatException, e -> PROJECT_NOT_FOUND_EXCEPTION)
                .flatMap(projectId -> Mono.zip(projectRepo.findById(projectId), pullRepo.countOpenOnes(projectId)))
                .map(tuple -> converter.convert(tuple.getT1(), tuple.getT2()))
                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
                .flatMap(project -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(project))
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build())
                .doOnSuccess(response -> log.debug("{}: {}", request, response.statusCode()));
    }

    public Mono<ServerResponse> create(final ServerRequest request) {
        return request
                .bodyToMono(CreateProjectRequestBody.class)
                .onErrorResume(e -> Mono.error(CreateProjectException.BAD_FORMAT))
                .map(ProjectsHandler::extractOwnerAndRepo)
                .flatMap(this::retrieveGithubInfoForProjectCreation)
                .onErrorMap(e -> e instanceof GithubClientException, e -> {
                    log.error("Github Api Client error:", e);
                    return CreateProjectException.WRONG_URL;
                })
                .flatMap(this::saveProjectData)
                .flatMap(this::createWebhook)
                .flatMap(project -> ServerResponse
                        .ok()
                        .contentType(APPLICATION_JSON)
                        .bodyValue(project))
                .doOnSuccess(response -> log.debug("{}: {}", request, response.statusCode()))
                .doOnError(t -> log.error("{}: {}", request, t))
                .onErrorResume(e -> e instanceof CreateProjectException,
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
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::parseLong)
                .onErrorMap(e -> e instanceof NumberFormatException, e -> PROJECT_NOT_FOUND_EXCEPTION)
                .zipWith(currentUser.get())
                .flatMap(projectIdAndCurrentUser -> {
                    final var projectId = projectIdAndCurrentUser.getT1();
                    final var currentUserId = projectIdAndCurrentUser.getT2().getId();

                    return projectRepo.delete(projectId, currentUserId);
                })
                .flatMap(success -> ServerResponse.ok().build())
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }

    private Mono<Tuple4<Boolean, GithubApiRepo, GithubApiPull[], User>> retrieveGithubInfoForProjectCreation(
            final Tuple2<String, String> ownerAndRepo) {
        final var owner = ownerAndRepo.getT1();
        final var repo = ownerAndRepo.getT2();

        //@formatter:off
        return Mono.zip(githubClient.hasAdminPermission(owner, repo).subscribeOn(remoteCallsScheduler),
                        githubClient.getRepo(owner, repo).subscribeOn(remoteCallsScheduler),
                        githubClient.getRepositoryPulls(owner, repo, State.ALL).subscribeOn(remoteCallsScheduler),
                        currentUser.get());
        //@formatter:on
    }

    private Mono<ProjectDto> saveProjectData(final Tuple4<Boolean, GithubApiRepo, GithubApiPull[], User> tuple) {
        return Mono.defer(() -> {
            final var isAdmin = tuple.getT1();
            if (!isAdmin) {
                throw CreateProjectException.NO_PERMISSION;
            }

            final var githubApiRepo = tuple.getT2();
            final var githubApiPulls = tuple.getT3();
            final var currentUser = tuple.getT4();

            final var projectGithubRepo = converter.convert(githubApiRepo);

            return githubUserRepo
                    .upsert(projectGithubRepo.getOwner())
                    .filterWhen(repoOwner -> projectRepo.notExists(projectGithubRepo.getId()))
                    .switchIfEmpty(Mono.error(CreateProjectException.ALREADY_EXISTS))
                    .flatMap(repoOwner -> projectRepo.upsert(projectGithubRepo, currentUser))
                    .flatMap(project -> saveProjectPulls(project, githubApiPulls)
                            .map(openPullCount -> converter.convert(project, openPullCount)));
        });
    }

    private Mono<Integer> saveProjectPulls(final Project project, final GithubApiPull[] githubApiPulls) {
        return Mono
                .defer(() -> {
                    Set<GithubUser> users = new HashSet<>();
                    Set<GithubRepo> repos = new HashSet<>();
                    Set<Commit> commits = new HashSet<>();
                    Set<Pull> pulls = new HashSet<>();

                    int openPullCount = 0;

                    for (final var githubApiPull : githubApiPulls) {
                        final var isPullOpen = githubApiPull.getState() == State.OPEN;
                        if (isPullOpen) {
                            ++openPullCount;
                        }

                        final var pullAuthor = converter.convert(githubApiPull.getUser());
                        users.add(pullAuthor);

                        final var head = githubApiPull.getHead();
                        final var headRepo = converter.convert(head.getRepo());
                        repos.add(headRepo);
                        users.add(headRepo.getOwner());
                        final var headCommit = new Commit(head.getSha());
                        commits.add(headCommit);

                        final var base = githubApiPull.getBase();
                        final var baseCommit = new Commit(base.getSha());
                        commits.add(baseCommit);

                        pulls.add(Pull.builder()
                                .id(githubApiPull.getId())
                                .number(githubApiPull.getNumber())
                                .title(githubApiPull.getTitle())
                                .open(isPullOpen)
                                .createdAt(githubApiPull.getCreatedAt())
                                .updatedAt(githubApiPull.getUpdatedAt())
                                .head(new Pull.Marker(headCommit, head.getRef(), headRepo))
                                .base(new Pull.Marker(baseCommit, base.getRef(), project.getGithubRepo()))
                                .projectId(project.getId())
                                .author(pullAuthor)
                                .build());
                    }

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .thenMany(commitRepo.upsert(commits))
                            .thenMany(pullRepo.upsert(pulls))
                            .then(Mono.just(openPullCount));
                })
                .subscribeOn(pullProcessingScheduler);
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
