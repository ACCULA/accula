package org.accula.api.db;

import org.accula.api.db.dto.User;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByGithubId(final Long githubId);

    <S extends User> Mono<S> save(final S user);

    @Modifying
    @Query("UPDATE users SET github_access_token = :githubAccessToken WHERE github_id = :githubId")
    Mono<Void> setNewAccessToken(final Long githubId, final String githubAccessToken);
}
