package org.accula.api.db;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
//FIXME: possible inconsistency
public final class CachingCurrentUserRepositoryImpl implements CurrentUserRepository {
    private final UserRepository users;
    private final Map<Long, User> cache = new ConcurrentHashMap<>();

    /**
     * @return current authorized user
     */
    @Override
    public Mono<User> get() {
        return CurrentAuthorizedUserProvider
                .get()
                .flatMap(authorizedUser -> Mono
                        .justOrEmpty(cache.get(authorizedUser.getId()))
                        .switchIfEmpty(users
                                .findById(authorizedUser.getId())
                                .doOnSuccess(user -> {
                                    if (user != null) {
                                        cache.put(authorizedUser.getId(), user);
                                    }
                                })));
    }
}
