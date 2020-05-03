package org.accula.api.db;

import org.accula.api.db.dto.RefreshToken;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * @author Anton Lamtev
 */
@Repository
public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {
    @Modifying
    @Query("UPDATE refresh_token SET token = :newToken WHERE user_id = :userId AND token = :oldToken")
    Mono<Void> replaceRefreshToken(final Long userId,
                                   final String oldToken,
                                   final String newToken,
                                   final Instant newExpirationDate);
}
