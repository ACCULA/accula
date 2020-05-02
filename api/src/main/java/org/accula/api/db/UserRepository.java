package org.accula.api.db;

import org.accula.api.db.dto.User;
import org.jetbrains.annotations.NotNull;
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
    @NotNull
    Mono<User> findByGithubId(@NotNull final Long githubId);

    @NotNull
    <S extends User> Mono<S> save(@NotNull final S user);

    @Modifying
    @Query("UPDATE users SET github_access_token = :githubAccessToken WHERE github_id = :githubId")
    @NotNull
    Mono<Void> setNewAccessToken(@NotNull final Long githubId, @NotNull final String githubAccessToken);
}
