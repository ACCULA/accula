package org.accula.api.handlers;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.CodeLoader;
import org.accula.api.code.FileEntity;
import org.accula.api.code.FileFilter;
import org.accula.api.db.CloneRepository;
import org.accula.api.db.CommitRepository;
import org.accula.api.db.ProjectRepository;
import org.accula.api.db.PullRepository;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.ProjectOld;
import org.accula.api.db.model.PullOld;
import org.accula.api.detector.CloneDetector;
import org.accula.api.detector.CodeSnippet;
import org.accula.api.github.model.GithubApiHookPayload;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Component
@RequiredArgsConstructor
public final class GithubWebhookHandler {
    private static final String GITHUB_EVENT = "X-GitHub-Event";
    private static final String GITHUB_EVENT_PING = "ping";

    private final ProjectRepository projectRepository;
    private final PullRepository pullRepository;
    private final CommitRepository commitRepository;
    private final CloneRepository cloneRepository;
    private final CloneDetector detector;
    private final CodeLoader loader;

    public Mono<ServerResponse> webhook(final ServerRequest request) {
        final var eventType = request.headers().firstHeader(GITHUB_EVENT);
        if (GITHUB_EVENT_PING.equals(eventType)) {
            return ServerResponse.ok().build();
        }
        // TODO: validate signature in X-Hub-Signature 
        return request
                .bodyToMono(GithubApiHookPayload.class)
                .flatMap(this::processPayload)
                .flatMap(p -> ServerResponse.ok().build());
    }

    public Mono<Void> processPayload(final GithubApiHookPayload payload) {
        final var projectOwner = payload.getRepository().getOwner().getLogin();
        final var projectRepo = payload.getRepository().getName();
        final var number = payload.getPull().getNumber();
        final var pullOwner = payload.getPull().getHead().getRepo().getOwner().getLogin();
        final var pullRepo = payload.getPull().getHead().getRepo().getName();
        final var headSha = payload.getPull().getHead().getSha();
        final var updatedAt = payload.getPull().getUpdatedAt();
        final var base = payload.getPull().getBase();

        // save to commit table & get commit with id
        final Mono<Commit> headCommit = commitRepository
                .findBySha(headSha)
                .switchIfEmpty(commitRepository.save(new Commit(null, pullOwner, pullRepo, headSha)))
                .cache();

        // update pull table
        final Mono<Long> projectId = projectRepository
                .findByRepoOwnerAndRepoName(projectOwner, projectRepo)
                .map(ProjectOld::getId)
                .cache();
        final Mono<PullOld> updatedPull = headCommit.flatMap(head -> projectId
                .flatMap(id -> pullRepository
                        .findByProjectIdAndNumber(id, number)
                        .switchIfEmpty(pullRepository.save(new PullOld(null, id, number, head.getId(), base.getSha(), updatedAt))))
                .flatMap(pull -> {
                    pull.setHeadLastCommitId(head.getId());
                    pull.setBaseLastCommitSha(base.getSha());
                    pull.setUpdatedAt(updatedAt);
                    return pullRepository.save(pull);
                }));

        // get previous commits
        final Flux<Commit> source = commitRepository.findAllById(projectId
                .flatMapMany(id -> pullRepository.findAllByProjectIdAndUpdatedAtBeforeAndNumberIsNot(id, updatedAt, number))
                .map(PullOld::getHeadLastCommitId));

        // get files by commits
        final Flux<FileEntity> targetFiles = headCommit
                .flatMapMany(commit -> loader.getFiles(commit, FileFilter.ALL));
        final Flux<FileEntity> sourceFiles = source
                .flatMap(commit -> loader.getFiles(commit, FileFilter.ALL));

        // find clones & save to db
        final Flux<Clone> clones = detector
                .findClones(targetFiles, sourceFiles)
                .map(this::convert);
        final Mono<Void> savedClones = cloneRepository
                .saveAll(clones)
                .then();

        return Mono.when(updatedPull, savedClones)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Clone convert(final Tuple2<CodeSnippet, CodeSnippet> clone) {
        final CodeSnippet target = clone.getT1();
        final CodeSnippet source = clone.getT2();
        return Clone.builder()
                .targetCommitId(target.getCommit().getId())
                .targetFile(target.getFile())
                .targetFromLine(target.getFromLine())
                .targetToLine(target.getToLine())
                .sourceCommitId(source.getCommit().getId())
                .sourceFile(source.getFile())
                .sourceFromLine(source.getFromLine())
                .sourceToLine(source.getToLine())
                .build();
    }
}
