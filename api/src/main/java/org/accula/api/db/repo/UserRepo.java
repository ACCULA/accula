package org.accula.api.db.repo;

import org.accula.api.db.model.User;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface UserRepo {
    Mono<User> upsert(User user);

    Mono<User> findById(Long id);

    Flux<User> findByGithubIds(Collection<Long> ids);

    Flux<User> findAll();

    Mono<List<User>> setAdminRole(Collection<Long> adminIds);

    void addOnUpsert(OnUpsert onUpsert);

    @FunctionalInterface
    interface OnUpsert {
        void onUpsert(@Nullable Long userId);

        static void forEach(final Iterable<? extends OnUpsert> onUpserts, @Nullable final Long userId) {
            onUpserts.forEach(onUpsert -> onUpsert.onUpsert(userId));
        }
    }
}
