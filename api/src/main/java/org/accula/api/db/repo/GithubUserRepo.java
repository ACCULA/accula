package org.accula.api.db.repo;

import org.accula.api.db.model.GithubUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface GithubUserRepo {
    default Mono<GithubUser> upsert(GithubUser user) {
        return upsert(List.of(user)).next();
    }

    Flux<GithubUser> upsert(Collection<GithubUser> users);

    Mono<GithubUser> findById(Long id);
}
