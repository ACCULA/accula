package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileFilter;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.CloneRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.detector.CloneDetector;
import org.accula.api.detector.CodeSnippet;
import org.accula.api.github.model.GithubApiHookPayload;
import org.accula.api.handlers.util.ProjectUpdater;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class GithubWebhookHandler {
    private static final String GITHUB_EVENT = "X-GitHub-Event";
    private static final String GITHUB_EVENT_PING = "ping";

    private final Scheduler processingScheduler = Schedulers.boundedElastic();
    private final ProjectRepo projectRepo;
    private final ProjectUpdater projectUpdater;
    private final PullRepo pullRepo;
    private final CloneRepo cloneRepo;
    private final CloneDetector detector;
    private final CodeLoader loader;

    public Mono<ServerResponse> webhook(final ServerRequest request) {
        if (GITHUB_EVENT_PING.equals(request.headers().firstHeader(GITHUB_EVENT))) {
            return ServerResponse.ok().build();
        }
        // TODO: validate signature in X-Hub-Signature 
        return request
                .bodyToMono(GithubApiHookPayload.class)
                .flatMap(this::processPayload)
                .onErrorResume(e -> {
                    log.error("Error: ", e);
                    return Mono.empty();
                })
                .flatMap(p -> ServerResponse.ok().build());
    }

    public Mono<Void> processPayload(final GithubApiHookPayload payload) {
        final var githubApiPull = payload.getPull();

        final var savedPull = projectRepo
                .idByRepoId(payload.getRepo().getId())
                .flatMap(projectId -> projectUpdater.update(projectId, githubApiPull))
                .cache();

        final var targetFiles = savedPull
                .map(Pull::getHead)
                .flatMapMany(head -> loader.getFiles(head, FileFilter.JAVA));

        final var sourceFiles = savedPull
                .flatMapMany(pull -> pullRepo.findUpdatedEarlierThan(pull.getProjectId(), pull.getNumber()))
                .map(Pull::getHead)
                .flatMap(head -> loader.getFiles(head, FileFilter.JAVA))
                .cache();

        final var cloneFlux = detector
                .findClones(targetFiles, sourceFiles)
                .map(this::convert);

        final var saveClones =  cloneFlux.collectList()
                .flatMap(clones -> cloneRepo.insert(clones).then());

        return Mono.when(savedPull, saveClones);
    }

    private Clone convert(final Tuple2<CodeSnippet, CodeSnippet> clone) {
        final CodeSnippet target = clone.getT1();
        final CodeSnippet source = clone.getT2();
        return Clone.builder()
                .targetSnapshot(target.getCommitSnapshot())
                .targetFile(target.getFile())
                .targetFromLine(target.getFromLine())
                .targetToLine(target.getToLine())
                .sourceSnapshot(source.getCommitSnapshot())
                .sourceFile(source.getFile())
                .sourceFromLine(source.getFromLine())
                .sourceToLine(source.getToLine())
                .build();
    }
}
