package org.accula.api.db.repo;

import org.accula.api.db.model.GithubRepo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface GithubRepoRepo {
    default Mono<GithubRepo> upsert(GithubRepo repo) {
        return upsert(List.of(repo)).next();
    }

    Flux<GithubRepo> upsert(Collection<GithubRepo> repos);

    Mono<GithubRepo> findById(Long id);
}
