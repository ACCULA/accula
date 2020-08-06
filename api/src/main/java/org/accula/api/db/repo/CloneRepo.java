package org.accula.api.db.repo;

import org.accula.api.db.model.Clone;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface CloneRepo {
    default Mono<Clone> insert(Clone clone) {
        return insert(List.of(clone)).next();
    }

    Flux<Clone> insert(Collection<Clone> clones);

    Mono<Clone> findById(Long id);

    Flux<Clone> findByTargetCommitSnapshotSha(String sha);

    Mono<Void> deleteByPullNumber(final long projectId, final int pullNumber);
}
