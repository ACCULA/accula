package org.accula.api.db;

import org.accula.api.db.model.User;
import reactor.core.publisher.Mono;

public interface UserRepo {
    Mono<Long> upsert(Long ghId,
                      String ghLogin,
                      String ghName,
                      String ghAvatar,
                      String ghAccessToken);

    Mono<User> get(Long id);

    void addOnUpsert(OnUpsert onUpsert);

    @FunctionalInterface
    interface OnUpsert {
        void onUpsert(Long userId);
    }
}
