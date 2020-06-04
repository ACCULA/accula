package org.accula.api.db;

import org.accula.api.db.model.UserOld;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface CurrentUserRepository {
    Mono<UserOld> get();
}
