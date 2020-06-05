package org.accula.api.db.repo;

import org.accula.api.db.model.GithubUser;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface GithubUserRepo {
    Mono<GithubUser> upsert(GithubUser user);

    Mono<GithubUser> findById(Long id);
}
