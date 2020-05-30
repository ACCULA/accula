package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.config.WebhookProperties;
import org.accula.api.db.CurrentUserRepository;
import org.accula.api.db.ProjectRepository;
import org.accula.api.db.PullRepository;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.User;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.api.GithubClientException;
import org.accula.api.github.model.GithubHook;
import org.accula.api.github.model.GithubPull;
import org.accula.api.github.model.GithubRepo;
import org.accula.api.handlers.request.CreateProjectRequestBody;
import org.accula.api.handlers.response.ErrorBody;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
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
    private final CurrentUserRepository currentUser;
    private final GithubClient githubClient;
    private final ProjectRepository projects;
    private final PullRepository pulls;

    public Mono<ServerResponse> getAll(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.queryParam("count").orElse("5"))
                .map(Integer::parseInt)
                .flatMap(count -> ServerResponse
                        .ok()
                        .body(projects.findAll().take(count), Project.class))
                .doOnSuccess(response -> log.debug("{}: {}", request, response.statusCode()));
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::parseLong)
                .onErrorMap(e -> e instanceof NumberFormatException, e -> PROJECT_NOT_FOUND_EXCEPTION)
                .flatMap(projects::findById)
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
                .filterWhen(ownerAndRepo -> projects
                        .notExistsByRepoOwnerAndRepoName(ownerAndRepo.getT1().toLowerCase(), ownerAndRepo.getT2().toLowerCase()))
                .switchIfEmpty(Mono.error(CreateProjectException.ALREADY_EXISTS))
                .flatMap(this::retrieveGithubInfoForProjectCreation)
                .onErrorMap(e -> e instanceof GithubClientException, e -> CreateProjectException.WRONG_URL)
                .map(ProjectsHandler::convertGithubResponse)
                .flatMap(this::saveProjectAndPulls)
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
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::parseLong)
                .onErrorMap(e -> e instanceof NumberFormatException, e -> PROJECT_NOT_FOUND_EXCEPTION)
                .zipWith(request.bodyToMono(Project.class))
                .switchIfEmpty(Mono.error(PROJECT_NOT_FOUND_EXCEPTION))
                .flatMap(projectAndCurrentUser -> {
                    final var projectId = projectAndCurrentUser.getT1();
                    final var project = projectAndCurrentUser.getT2();
                    final var admins = project.getAdmins();

                    return currentUser
                            .get()
                            .flatMap(currentUser -> projects.
                                    setAdmins(projectId, admins, requireNonNull(currentUser.getId())));
                })
                .flatMap(nil -> ServerResponse.ok().build())
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> delete(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("id"))
                .map(Long::parseLong)
                .onErrorMap(e -> e instanceof NumberFormatException, e -> PROJECT_NOT_FOUND_EXCEPTION)
                .zipWith(currentUser.get())
                .flatMap(projectIdAndCurrentUser -> {
                    final var projectId = projectIdAndCurrentUser.getT1();
                    final var userId = requireNonNull(projectIdAndCurrentUser.getT2().getId());

                    return projects.deleteByIdAndCreatorId(projectId, userId);
                })
                .flatMap(success -> ServerResponse.ok().build())
                .onErrorResume(PROJECT_NOT_FOUND_EXCEPTION::equals, e -> ServerResponse.notFound().build());
    }

    private Mono<Tuple4<Boolean, GithubRepo, GithubPull[], User>> retrieveGithubInfoForProjectCreation(
            final Tuple2<String, String> ownerAndRepo) {
        final var owner = ownerAndRepo.getT1();
        final var repo = ownerAndRepo.getT2();
        final var scheduler = Schedulers.boundedElastic();

        //@formatter:off
        return Mono.zip(githubClient.hasAdminPermission(owner, repo).subscribeOn(scheduler),
                        githubClient.getRepo(owner, repo).subscribeOn(scheduler),
                        githubClient.getRepositoryOpenPulls(owner, repo).subscribeOn(scheduler),
                        currentUser.get());
        //@formatter:on
    }

    private Mono<Project> saveProjectAndPulls(final Tuple2<Project, Iterable<Pull>> projectAndPulls) {
        final var project = projectAndPulls.getT1();
        final var openPulls = projectAndPulls.getT2();

        return projects
                .save(project)
                .doOnSuccess(savedProject -> openPulls
                        .forEach(pull -> pull.setProjectId(savedProject.getId())))
                .flatMap(savedProject -> pulls
                        .saveAll(openPulls)
                        .then(Mono.just(savedProject)));
    }

    private Mono<Project> createWebhook(final Project project) {
        final var hook = GithubHook.onPullUpdates(webhookProperties.getUrl(), webhookProperties.getSecret());
        return githubClient.createHook(project.getRepoOwner(), project.getRepoName(), hook)
                .doOnSuccess(p -> log.info("Created GitHub webhook for projectId={}", project.getId()))
                .doOnError(e -> log.error("Cannot create hook for projectId={}", project.getId(), e))
                .thenReturn(project);
    }

    private static Tuple2<Project, Iterable<Pull>> convertGithubResponse(final Tuple4<Boolean, GithubRepo, GithubPull[], User> tuple) {
        final var isAdmin = tuple.getT1();

        if (!isAdmin) {
            throw CreateProjectException.NO_PERMISSION;
        }

        final var repo = tuple.getT2();
        final var openPulls = tuple.getT3();
        final var currentUser = tuple.getT4();
        final var creatorId = requireNonNull(currentUser.getId());
        final var repoUrl = repo.getHtmlUrl();
        final var repoName = repo.getName();
        final var repoDescription = Optional.ofNullable(repo.getDescription()).orElse("");
        final var repoOpenPullCount = openPulls.length;
        final var repoOwnerObj = repo.getOwner();
        final var repoOwner = repoOwnerObj.getLogin();
        final var repoOwnerAvatar = repoOwnerObj.getAvatarUrl();

        final var project = Project.builder()
                .creatorId(creatorId)
                .repoUrl(repoUrl)
                .repoName(repoName.toLowerCase())
                .repoDescription(repoDescription)
                .repoOpenPullCount(repoOpenPullCount)
                .repoOwner(repoOwner.toLowerCase())
                .repoOwnerAvatar(repoOwnerAvatar)
                .build();

        final var pulls = Stream
                .of(openPulls)
                .map(ghPull -> new Pull(null, -1L, ghPull.getNumber(), null, ghPull.getUpdatedAt()))
                .collect(toList());

        return Tuples.of(project, pulls);
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
