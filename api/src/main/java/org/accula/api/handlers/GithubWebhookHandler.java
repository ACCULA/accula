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
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.detector.CloneDetector;
import org.accula.api.detector.CodeSnippet;
import org.accula.api.github.model.GithubHookPayload;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.time.Instant;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@Component
@RequiredArgsConstructor
public final class GithubWebhookHandler {
    private final ProjectRepository projectRepository;
    private final PullRepository pullRepository;
    private final CommitRepository commitRepository;
    private final CloneRepository cloneRepository;
    private final CloneDetector detector;
    private final CodeLoader loader;

    public Mono<ServerResponse> webhook(final ServerRequest request) {
        return request
                .bodyToMono(GithubHookPayload.class)
                .flatMap(this::processCommit)
                .flatMap(p -> ServerResponse.ok().build());
    }

    public Mono<Void> processCommit(final GithubHookPayload p) {
        final String projectOwner = p.getRepository().getOwner().getLogin();
        final String projectRepo = p.getRepository().getName();
        final Integer number = p.getPull().getNumber();
        final String pullOwner = p.getPull().getHead().getRepo().getOwner().getLogin();
        final String pullRepo = p.getPull().getHead().getRepo().getName();
        final String sha = p.getPull().getHead().getSha();
        final Instant updatedAt = p.getPull().getUpdatedAt();

        // save to commit table & get commit with id
        final Mono<Commit> target = commitRepository
                .save(new Commit(null, pullOwner, pullRepo, sha))
                .cache();

        // update pull table
        final Mono<Long> projectId = projectRepository
                .findByRepoOwnerAndRepoName(projectOwner, projectRepo)
                .map(Project::getId)
                .cache();
        final Mono<Pull> updatedPull = projectId
                .flatMap(id -> pullRepository.findByProjectIdAndNumber(id, number))
                .zipWith(target)
                .flatMap(pullAndCommit -> {
                    Pull pull = pullAndCommit.getT1();
                    Long commitId = pullAndCommit.getT2().getId();
                    pull.setLastCommitId(commitId);
                    return pullRepository.save(pull);
                });

        // get previous commits
        final Flux<Commit> source = projectId
                .flatMapMany(id -> pullRepository.findAllByProjectIdAndUpdatedAtBeforeAndNumberIsNot(id, updatedAt, number))
                .filter(pull -> pull.getLastCommitId() != null)
                .map(Pull::getLastCommitId)
                .flatMap(commitRepository::findById);

        // get files by commits
        final Flux<FileEntity> targetFiles = target
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