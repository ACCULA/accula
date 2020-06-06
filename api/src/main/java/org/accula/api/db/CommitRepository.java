package org.accula.api.db;

import org.accula.api.db.model.CommitOld;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
public interface CommitRepository extends ReactiveCrudRepository<CommitOld, Long> {
    Mono<CommitOld> findBySha(String sha);
}
