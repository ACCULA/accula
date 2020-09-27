package org.accula.api.db.repo;

import org.accula.api.db.model.Snapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface CommitSnapshotRepo {
    default Mono<Snapshot> insert(Snapshot snapshot) {
        return insert(List.of(snapshot)).next();
    }

    Flux<Snapshot> insert(Collection<Snapshot> snapshots);

    Flux<Snapshot> mapToPulls(Collection<Snapshot> snapshots);

    default Mono<Snapshot> findById(Snapshot.Id id) {
        return findById(List.of(id)).next();
    }

    Flux<Snapshot> findById(Collection<Snapshot.Id> ids);
}
