package org.accula.api.db.repo;

import org.accula.api.db.model.User;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface CurrentUserRepo {
    /**
     * @return current authorized user
     */
    Mono<User> get();
}
