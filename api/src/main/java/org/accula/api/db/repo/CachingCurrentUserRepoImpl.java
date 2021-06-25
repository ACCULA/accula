package org.accula.api.db.repo;

import org.accula.api.auth.CurrentAuthorizedUserProvider;
import org.accula.api.db.model.User;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Anton Lamtev
 */
@Component
public final class CachingCurrentUserRepoImpl implements CurrentUserRepo {
    private final Map<Long, User> cache = new ConcurrentHashMap<>();
    private final UserRepo userRepo;

    public CachingCurrentUserRepoImpl(final UserRepo userRepo) {
        this.userRepo = userRepo;
        this.userRepo.addOnUpsert(this::evict);
    }

    @Override
    public Mono<User> get() {
        return CurrentAuthorizedUserProvider
                .get()
                .flatMap(authorizedUser -> Mono
                        .justOrEmpty(cache.get(authorizedUser.id()))
                        .switchIfEmpty(userRepo
                                .findById(authorizedUser.id())
                                .doOnNext(user -> cache.put(authorizedUser.id(), user))));
    }

    private void evict(@Nullable final Long userId) {
        if (userId == null) {
            cache.clear();
            return;
        }
        cache.remove(userId);
    }
}
