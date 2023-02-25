package org.accula.api.service;

import org.accula.api.code.CodeLoader;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.PullSnapshots;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.db.repo.SnapshotRepo;
import org.accula.api.routers.GithubWebhookRouterTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Anton Lamtev
 */
@WebFluxTest
@ContextConfiguration(classes = {
    ProjectService.class,
})
class ProjectServiceTest {
    @Autowired
    ProjectService projectService;
    @MockBean
    ProjectRepo projectRepo;
    @MockBean
    GithubUserRepo githubUserRepo;
    @MockBean
    GithubRepoRepo githubRepoRepo;
    @MockBean
    SnapshotRepo snapshotRepo;
    @MockBean
    PullRepo pullRepo;
    @MockBean
    CodeLoader codeLoader;
    @MockBean
    CloneDetectionService cloneDetectionService;
    @MockBean
    CurrentUserRepo currentUserRepo;

    @Test
    void testInitSinglePull() {
        final var pull = GithubApiToModelConverter.convert(GithubWebhookRouterTest.pullOpenedPayload.pull());
        final var snap = pull.head();
        when(githubUserRepo.upsert(anyCollection()))
            .thenReturn(Flux.empty());
        when(githubRepoRepo.upsert(anyCollection()))
            .thenReturn(Flux.empty());
        when(codeLoader.loadCommits(any(), anyString()))
            .thenReturn(Flux.just(snap.commit()));
        when(snapshotRepo.insert(anyIterable()))
            .thenReturn(Flux.empty());
        when(pullRepo.upsert(any(Pull.class)))
            .thenReturn(Mono.empty());
        when(pullRepo.mapSnapshots(any(PullSnapshots.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(projectService.init(GithubWebhookRouterTest.pullOpenedPayload.pull()))
            .expectNext(new PullSnapshots(pull, List.of(snap)))
            .verifyComplete();
    }

    @Test
    void testUpdateWithNewPulls() {
        final var pull = GithubApiToModelConverter.convert(GithubWebhookRouterTest.pullSynchronizePayload.pull());

        when(githubUserRepo.upsert(anyCollection()))
            .thenReturn(Flux.empty());
        when(githubRepoRepo.upsert(anyCollection()))
            .thenReturn(Flux.empty());
        when(pullRepo.findById(anyLong()))
            .thenReturn(Mono.just(pull));
        when(codeLoader.loadCommit(any(), anyString()))
            .thenReturn(Mono.just(pull.base().commit()));
        when(codeLoader.loadCommits(any(), anyString(), anyString()))
            .thenReturn(Flux.just(pull.head().commit(), pull.base().commit()));
        when(snapshotRepo.insert(anyIterable()))
            .thenReturn(Flux.empty());
        when(pullRepo.upsert(any(Pull.class)))
            .thenReturn(Mono.empty());
        when(pullRepo.mapSnapshots(any(PullSnapshots.class)))
            .thenReturn(Mono.empty());

        StepVerifier.create(projectService.updateWithNewCommits(GithubWebhookRouterTest.pullSynchronizePayload.pull()))
            .expectNext(new PullSnapshots(pull, List.of(pull.head(), pull.head().withCommit(pull.base().commit()))))
            .verifyComplete();
    }

    @Test
    void testUpdatePullInfo() {
        final var pull = GithubApiToModelConverter.convert(GithubWebhookRouterTest.assignedPayload.pull());

        when(githubUserRepo.upsert(anyCollection()))
            .thenReturn(Flux.empty());
        when(githubRepoRepo.upsert(anyCollection()))
            .thenReturn(Flux.empty());
        when(pullRepo.upsert(pull))
            .thenReturn(Mono.just(pull));

        StepVerifier.create(projectService.updatePullInfo(GithubWebhookRouterTest.assignedPayload.pull()))
            .expectNext(pull)
            .verifyComplete();
    }

    @Test
    void testHeadFiles() {
        when(codeLoader.loadFilenames(any()))
            .thenReturn(Flux.just("f2", "f1"));

        StepVerifier.create(projectService.headFiles(any()))
            .expectNext(List.of(Project.Conf.KEEP_EXCLUDED_FILES_SYNCED, "f1", "f2"))
            .verifyComplete();
    }
}
