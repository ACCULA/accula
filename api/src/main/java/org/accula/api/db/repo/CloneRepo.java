package org.accula.api.db.repo;

import org.accula.api.db.model.Clone;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
public interface CloneRepo {
    Mono<Clone> insert(Clone clone);

    Flux<Clone> insert(Collection<Clone> clones);

    Mono<Clone> findById(Long id);

    Flux<Clone> findByTargetCommitSnapshotSha(String sha);
}
