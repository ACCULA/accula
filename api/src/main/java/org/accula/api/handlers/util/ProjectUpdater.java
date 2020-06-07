package org.accula.api.handlers.util;

import lombok.RequiredArgsConstructor;
import org.accula.api.converter.GithubApiToModelConverter;
import org.accula.api.db.model.Commit;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.repo.CommitRepo;
import org.accula.api.db.repo.GithubRepoRepo;
import org.accula.api.db.repo.GithubUserRepo;
import org.accula.api.db.repo.PullRepo;
import org.accula.api.github.model.GithubApiPull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class ProjectUpdater {
    private final Scheduler processingScheduler = Schedulers.boundedElastic();
    private final GithubApiToModelConverter converter;
    private final GithubUserRepo githubUserRepo;
    private final GithubRepoRepo githubRepoRepo;
    private final CommitRepo commitRepo;
    private final PullRepo pullRepo;

    public Mono<Integer> update(final Project project, final GithubApiPull[] githubApiPulls) {
        if (githubApiPulls.length == 0) {
            return Mono.just(0);
        }

        return Mono
                .defer(() -> {
                    final var users = new HashSet<GithubUser>();
                    final var repos = new HashSet<GithubRepo>();
                    final var commits = new HashSet<Commit>();
                    final var pulls = new HashSet<Pull>();
                    int openPullCount = 0;

                    final var baseRepo = converter.convert(githubApiPulls[0].getBase().getRepo());
                    users.add(baseRepo.getOwner());
                    repos.add(baseRepo);

                    for (final var githubApiPull : githubApiPulls) {
                        if (!githubApiPull.isValid()) {
                            continue;
                        }

                        final var head = githubApiPull.getHead();
                        final var headRepo = converter.convert(head.getRepo());
                        users.add(headRepo.getOwner());
                        repos.add(headRepo);
                        final var headCommit = new Commit(head.getSha());
                        commits.add(headCommit);

                        final var base = githubApiPull.getBase();
                        final var baseCommit = new Commit(base.getSha());
                        commits.add(baseCommit);

                        final var isPullOpen = githubApiPull.getState() == GithubApiPull.State.OPEN;
                        final var pullAuthor = converter.convert(githubApiPull.getUser());
                        users.add(pullAuthor);

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

                        if (isPullOpen) {
                            ++openPullCount;
                        }
                    }

                    return githubUserRepo.upsert(users)
                            .thenMany(githubRepoRepo.upsert(repos))
                            .thenMany(commitRepo.upsert(commits))
                            .thenMany(pullRepo.upsert(pulls))
                            .then(Mono.just(openPullCount));
                })
                .subscribeOn(processingScheduler);
    }
}
