package org.accula.api.db;

import org.accula.api.db.model.PullOld;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
public interface PullRepository extends ReactiveCrudRepository<PullOld, Long> {
    @Query("SELECT exists(SELECT 0 FROM pull WHERE project_id = :projectId AND number = :number)")
    Mono<Boolean> existsByProjectIdAndNumber(Long projectId, Integer number);

    Mono<PullOld> findByProjectIdAndNumber(Long projectId, Integer number);

    Flux<PullOld> findAllByProjectIdAndUpdatedAtBeforeAndNumberIsNot(Long projectId, Instant updateAt, Integer number);
}
