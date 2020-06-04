package org.accula.api.db;

import org.accula.api.db.model.User;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

public interface UserRepo {
    Mono<Long> upsert(Long ghId,
                      String ghLogin,
                      @Nullable String ghName,
                      String ghAvatar,
                      String ghAccessToken);

    Mono<User> get(Long id);
}
