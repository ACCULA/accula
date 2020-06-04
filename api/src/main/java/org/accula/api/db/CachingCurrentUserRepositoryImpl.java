package org.accula.api.db;

import org.accula.api.auth.CurrentAuthorizedUserProvider;
import org.accula.api.db.model.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Anton Lamtev
 */
@Component
public final class CachingCurrentUserRepositoryImpl implements CurrentUserRepository {
    private final Map<Long, User> cache = new ConcurrentHashMap<>();
    private final UserRepo userRepo;

    public CachingCurrentUserRepositoryImpl(final UserRepo userRepo) {
        this.userRepo = userRepo;
        this.userRepo.addOnUpsert(this::evict);
    }

    @Override
    public Mono<User> get() {
        return CurrentAuthorizedUserProvider
                .get()
                .flatMap(authorizedUser -> Mono
                        .justOrEmpty(cache.get(authorizedUser.getId()))
                        .switchIfEmpty(userRepo
                                .get(authorizedUser.getId())
                                .doOnSuccess(user -> {
                                    if (user != null) {
                                        cache.put(authorizedUser.getId(), user);
                                    }
                                })));
    }

    private void evict(final Long userId) {
        cache.remove(userId);
    }
}
