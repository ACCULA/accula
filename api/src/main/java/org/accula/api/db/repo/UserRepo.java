package org.accula.api.db.repo;

import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.User;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface UserRepo {
    Mono<User> upsert(GithubUser githubUser, String githubAccessToken);

    Mono<User> findById(Long id);

    void addOnUpsert(OnUpsert onUpsert);

    @FunctionalInterface
    interface OnUpsert {
        void onUpsert(Long userId);
    }
}
