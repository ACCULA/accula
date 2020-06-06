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
import org.accula.api.handlers.request.CreateProjectRequestBody;
import org.accula.api.handlers.response.ErrorBody;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
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

    public Mono<ServerResponse> getTop(final ServerRequest request) {
        return Mono
                .just(request.queryParam("count").orElse("5"))
                .map(Integer::parseInt)
                .flatMap(count -> ServerResponse
                        .ok()
                        .body(projectRepo.getTop(count), Project.class))
                .doOnSuccess(response -> log.debug("{}: {}", request, response.statusCode()));
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::parseLong)
                .onErrorMap(e -> e instanceof NumberFormatException, e -> PROJECT_NOT_FOUND_EXCEPTION)
                .flatMap(projectRepo::get)
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
                .onErrorMap(e -> e instanceof GithubClientException, e -> CreateProjectException.WRONG_URL)
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

    // Only admin updating is currently supported
    public Mono<ServerResponse> update(final ServerRequest request) {
        return Mono.empty();
//                .justOrEmpty(request.pathVariable("id"))
//                .map(Long::parseLong)
//                .onErrorMap(e -> e instanceof NumberFormatException, e -> PROJECT_NOT_FOUND_EXCEPTION)
//                .zipWith(request.bodyToMono(ProjectOld.class))
//                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
//                .flatMap(projectAndCurrentUser -> {
//                    final var projectId = projectAndCurrentUser.getT1();
//                    final var project = projectAndCurrentUser.getT2();
//                    final var admins = project.getAdmins();
//
//                    return currentUser.get()
//                            .flatMap(currentUser -> projectRepository.
//                                    setAdmins(projectId, admins, requireNonNull(currentUser.getId())));
//                })
//                .flatMap(nil -> ServerResponse.ok().build())
//                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> delete(final ServerRequest request) {
        return Mono.empty();
//                .justOrEmpty(request.pathVariable("id"))
//                .map(Long::parseLong)
//                .onErrorMap(e -> e instanceof NumberFormatException, e -> PROJECT_NOT_FOUND_EXCEPTION)
//                .zipWith(currentUser.get())
//                .flatMap(projectIdAndCurrentUser -> {
//                    final var projectId = projectIdAndCurrentUser.getT1();
//                    final var userId = requireNonNull(projectIdAndCurrentUser.getT2().getId());
//
//                    return projectRepository.deleteByIdAndCreatorId(projectId, userId);
//                })
//                .flatMap(success -> ServerResponse.ok().build())
//                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
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

    private Mono<Project> saveProjectData(final Tuple4<Boolean, GithubApiRepo, GithubApiPull[], User> tuple) {
        final var isAdmin = tuple.getT1();
        if (!isAdmin) {
            throw CreateProjectException.NO_PERMISSION;
        }

        final var ghRepo = tuple.getT2();
        final var ghPulls = tuple.getT3();
        final var creator = tuple.getT4();


        final var githubRepo = converter.convert(ghRepo);

//        final var project = projectAndPulls.getT1();
//        final var ghPulls = Arrays.stream(projectAndPulls.getT2())
//                .filter(GithubPull::isValid)
//                .collect(toList());
//
//        final var commits = ghPulls
//                .stream()
//                .map(ghPull -> {
//                    final var head = ghPull.getHead();
//                    final var repo = head.getRepo();
//                    return new Commit(null, repo.getOwner().getLogin(), repo.getName(), head.getSha());
//                })
//                .collect(toList());

        return githubUserRepo
                .upsert(githubRepo.getOwner())
                .filterWhen(repoOwner -> projectRepo.notExists(githubRepo.getId()))
                .switchIfEmpty(Mono.error(CreateProjectException.ALREADY_EXISTS))
                .flatMap(repoOwner -> projectRepo.upsert(githubRepo, creator))
                .flatMap(project -> {
                    Set<GithubUser> users = new HashSet<>();
                    Set<GithubRepo> repos = new HashSet<>();
                    Set<Commit> commits = new HashSet<>();
                    Set<Pull> pulls = new HashSet<>();

                    for (final var ghPull : ghPulls) {
                        final var pullUser = converter.convert(ghPull.getUser());
                        users.add(pullUser);
                        final var head = ghPull.getHead();
                        users.add(converter.convert(head.getUser()));
                        final var headApiRepo = head.getRepo();
                        final var headUser = converter.convert(headApiRepo.getOwner());
                        users.add(headUser);
                        final var headRepo = converter.convert(headApiRepo);

                        repos.add(headRepo);
                        final var headCommit = new Commit(head.getSha(), headRepo);
                        commits.add(headCommit);

                        final var base = ghPull.getBase();
                        final var baseCommit = new Commit(base.getSha(), githubRepo);
                        commits.add(baseCommit);
                        final var baseUser = converter.convert(base.getUser());
                        users.add(baseUser);

                        pulls.add(Pull.builder()
                                .id(ghPull.getId())
                                .number(ghPull.getNumber())
                                .title(ghPull.getTitle())
                                .open(ghPull.getState() == State.OPEN)
                                .createdAt(ghPull.getCreatedAt())
                                .updatedAt(ghPull.getUpdatedAt())
                                .head(new Pull.Marker(headCommit, head.getRef(), headRepo, headUser))
                                .base(new Pull.Marker(baseCommit, base.getRef(), githubRepo, baseUser))
                                .project(project)
                                .author(pullUser)
                                .build());
                    }
                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .thenMany(commitRepo.upsert(commits))
                            .thenMany(pullRepo.upsert(pulls))
                            .then(Mono.just(project));
                });
    }

    private Mono<Project> createWebhook(final Project project) {
        final var hook = GithubApiHook.onPullUpdates(webhookProperties.getUrl(), webhookProperties.getSecret());
        return githubClient
                .createHook(project.getGithubRepo().getOwner().getLogin(), project.getGithubRepo().getName(), hook)
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
