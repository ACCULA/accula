package org.accula.api.db.repo;

import org.accula.api.db.model.GithubRepo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface GithubRepoRepo {
    Mono<GithubRepo> upsert(GithubRepo repo);

    Flux<GithubRepo> upsert(Collection<GithubRepo> repos);

    Mono<GithubRepo> findById(Long id);
}
