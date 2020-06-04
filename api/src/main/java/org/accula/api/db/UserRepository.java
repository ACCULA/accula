package org.accula.api.db;

import org.accula.api.db.model.UserOld;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<UserOld, Long> {
    Mono<UserOld> findByGithubId(Long githubId);

    //@formatter:off
    @Modifying
    @Query("UPDATE users " +
           "SET github_login = :githubLogin, github_access_token = :githubAccessToken " +
           "WHERE github_id = :githubId")
    Mono<Void> updateGithubLoginAndAccessToken(Long githubId, String githubLogin, String githubAccessToken);
    //@formatter:on
}
