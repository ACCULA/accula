package org.accula.api.db;

import org.accula.api.db.model.Pull;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface PullRepository extends ReactiveCrudRepository<Pull, Long> {
    @Query("SELECT exists(SELECT 0 FROM pull WHERE project_id = :projectId AND number = :number)")
    Mono<Boolean> existsByProjectIdAndNumber(final Long projectId, final Integer number);
}
