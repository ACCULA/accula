package org.accula.api.db.repo;

import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface ProjectRepo {
    Mono<Boolean> notExists(Long githubRepoId);

    /**
     * Upserts the project with the underlying github repo in one request to DB
     */
    Mono<Project> upsert(GithubRepo githubRepo, User creator);

    Mono<Project> findById(Long id);

    Mono<Long> idByRepoId(Long repoId);

    Flux<Project> getTop(int count);

    Mono<Boolean> delete(Long id, Long creatorId);
}
