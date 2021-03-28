package org.accula.api.db.repo;

import org.accula.api.db.model.Snapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface SnapshotRepo {
    default Mono<Snapshot> insert(final Snapshot snapshot) {
        return insert(List.of(snapshot)).next();
    }

    Flux<Snapshot> insert(Iterable<Snapshot> snapshots);

    default Mono<Snapshot> findById(final Snapshot.Id id) {
        return findById(List.of(id)).next();
    }

    Flux<Snapshot> findById(Collection<Snapshot.Id> ids);

    Flux<Snapshot> findByRepoId(Long repoId);

    Flux<Snapshot> findByPullId(Long pullId);
}
