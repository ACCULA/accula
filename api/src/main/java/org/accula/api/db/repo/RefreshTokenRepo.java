package org.accula.api.db.repo;

import org.accula.api.db.model.RefreshToken;
import org.intellij.lang.annotations.Language;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * @author Anton Lamtev
 */
public interface RefreshTokenRepo extends ReactiveCrudRepository<RefreshToken, Long> {
    @Language("SQL")
    String REPLACE_QUERY = """
            UPDATE refresh_token
            SET token = :newToken, expiration_date = :newExpirationDate
            WHERE user_id = :userId AND token = :oldToken
            """;

    @Modifying
    @Query(REPLACE_QUERY)
    Mono<Void> replaceRefreshToken(Long userId, String oldToken, String newToken, Instant newExpirationDate);
}
