package org.accula.api.db.repo;

import org.accula.api.db.model.User;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author Anton Lamtev
 */
public interface CurrentUserRepo {
    /**
     * @return current authorized user
     */
    Mono<User> get();

    default <R> Mono<R> get(Function<User, R> keyPath) {
        return get().map(keyPath);
    }
}
