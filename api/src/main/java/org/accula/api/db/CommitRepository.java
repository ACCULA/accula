package org.accula.api.db;

import org.accula.api.db.model.Commit;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
public interface CommitRepository extends ReactiveCrudRepository<Commit, Long> {
    Mono<Commit> findBySha(String sha);
}
