package org.accula.api.db;

import org.accula.api.db.model.Clone;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
public interface CloneRepository extends ReactiveCrudRepository<Clone, Long> {
    Flux<Clone> findAllByTargetCommitSha(String targetCommitSha);
}
