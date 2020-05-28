package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.db.CommitRepository;
import org.accula.api.db.ProjectRepository;
import org.accula.api.db.PullRepository;
import org.accula.api.db.model.Commit;
import org.accula.api.github.api.GithubClient;
import org.accula.api.github.model.GithubPull;
import org.accula.api.handlers.response.GetDiffResponseBody;
import org.accula.api.handlers.response.GetPullResponseBody;
import org.accula.api.handlers.utils.Base64Encoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static java.lang.Boolean.TRUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class PullsHandler {
    //TODO: common handler for all NOT FOUND cases
    private static final Exception PULL_NOT_FOUND_EXCEPTION = new Exception();

    private final ProjectRepository projects;
    private final CommitRepository commitRepository;
    private final PullRepository pulls;
    private final GithubClient githubClient;
    private final CodeLoader codeLoader;
    private final Base64Encoder base64;

    //TODO: handle github errors
    public Mono<ServerResponse> getAll(final ServerRequest request) {
        return Mono
                .fromSupplier(() -> Long.parseLong(request.pathVariable("projectId")))
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .flatMap(projectId -> projects
                        .findById(projectId)
                        .flatMap(project -> githubClient.getRepositoryOpenPulls(project.getRepoOwner(), project.getRepoName()))
                        .switchIfEmpty(Mono.error(PULL_NOT_FOUND_EXCEPTION))
                        .flatMapMany(Flux::fromArray)
                        .flatMap(pull -> Mono.just(fromGithubPull(pull, projectId)))
                        .collectList()
                        .flatMap(githubPulls -> ServerResponse
                                .ok()
                                .contentType(APPLICATION_JSON)
                                .bodyValue(githubPulls)))
                .onErrorResume(e -> e == PULL_NOT_FOUND_EXCEPTION, e -> ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> get(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable("projectId"));
                    final var pullNumber = Integer.parseInt(request.pathVariable("pullNumber"));

                    return pulls
                            .existsByProjectIdAndNumber(projectId, pullNumber)
                            .filter(TRUE::equals)
                            .flatMap(exists -> projects.findById(projectId))
                            .flatMap(project -> githubClient.getRepositoryPull(project.getRepoOwner(), project.getRepoName(), pullNumber))
                            .switchIfEmpty(Mono.error(PULL_NOT_FOUND_EXCEPTION))
                            .flatMap(githubPull -> ServerResponse
                                    .ok()
                                    .contentType(APPLICATION_JSON)
                                    .bodyValue(fromGithubPull(githubPull, projectId)));
                })
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .onErrorResume(e -> e == PULL_NOT_FOUND_EXCEPTION, e -> ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> getDiff(final ServerRequest request) {
        return Mono
                .defer(() -> {
                    final var projectId = Long.parseLong(request.pathVariable("projectId"));
                    final var pullNumber = Integer.parseInt(request.pathVariable("pullNumber"));
                    final var baseSha = request.queryParam("sha").orElseThrow();
                    final var base = projects.findById(projectId)
                            .map(p -> new Commit(-1L, p.getRepoOwner(), p.getRepoName(), baseSha));

                    final var head = pulls.findByProjectIdAndNumber(projectId, pullNumber)
                            .map(pull -> Mono.justOrEmpty(pull.getLastCommitId()))
                            .flatMap(commitRepository::findById);

                    return Mono.zip(base, head)
                            .flatMapMany(headBase -> codeLoader.getDiff(headBase.getT1(), headBase.getT2()))
                            .map(this::toResponseBody)
                            .collectList()
                            .flatMap(diffs -> ServerResponse
                                    .ok()
                                    .contentType(APPLICATION_JSON)
                                    .bodyValue(diffs));
                })
                .onErrorMap(NumberFormatException.class, e -> PULL_NOT_FOUND_EXCEPTION)
                .onErrorResume(e -> e == PULL_NOT_FOUND_EXCEPTION, e -> ServerResponse.notFound().build());
    }

    private GetDiffResponseBody toResponseBody(Tuple2<FileEntity, FileEntity> diff) {
        final var base = diff.getT1();
        final var head = diff.getT2();
        return GetDiffResponseBody.builder()
                .baseFilename(base.getName())
                .baseContent(base64.encode(base.getContent()))
                .headFilename(head.getName())
                .headContent(base64.encode(head.getContent()))
                .build();
    }

    //TODO
    public Mono<ServerResponse> refresh(final ServerRequest request) {
        return Mono
                .justOrEmpty(request.pathVariable("pullNumber"))
                //switchIfEmpty
                .map(Long::parseLong)
                //onError
                .flatMap(projects::findById)
                .then(Mono.empty());
    }

    private static GetPullResponseBody fromGithubPull(final GithubPull githubPull, final Long projectId) {
        return GetPullResponseBody.builder()
                .projectId(projectId)
                .number(githubPull.getNumber())
                .url(githubPull.getHtmlUrl())
                .title(githubPull.getTitle())
                .source(new GetPullResponseBody.PullRef(
                        githubPull.getHead().getTreeUrl(),
                        githubPull.getHead().getLabel()))
                .target(new GetPullResponseBody.PullRef(
                        githubPull.getBase().getTreeUrl(),
                        githubPull.getBase().getLabel()))
                .author(new GetPullResponseBody.PullAuthor(
                        githubPull.getUser().getLogin(),
                        githubPull.getUser().getAvatarUrl(),
                        githubPull.getUser().getHtmlUrl()))
                .open(githubPull.getState() == GithubPull.State.OPEN)
                .createdAt(githubPull.getCreatedAt())
                .updatedAt(githubPull.getUpdatedAt())
                .status(GetPullResponseBody.PullStatus.PENDING)
                .cloneCount(0)
                .build();
    }
}
