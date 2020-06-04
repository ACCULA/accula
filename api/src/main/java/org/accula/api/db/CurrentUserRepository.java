package org.accula.api.db;

import org.accula.api.db.model.User;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface CurrentUserRepository {
    /**
     * @return current authorized user
     */
    Mono<User> get();
}
