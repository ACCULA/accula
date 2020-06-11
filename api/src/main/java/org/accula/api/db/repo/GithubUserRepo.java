package org.accula.api.db.repo;

import org.accula.api.db.model.GithubUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
public interface GithubUserRepo {
    Mono<GithubUser> upsert(GithubUser user);

    Flux<GithubUser> upsert(Collection<GithubUser> users);

    Mono<GithubUser> findById(Long id);
}
