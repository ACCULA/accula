package org.accula.api.db.repo;

import org.accula.api.db.model.CommitSnapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
public interface CommitSnapshotRepo {
    Mono<CommitSnapshot> insert(CommitSnapshot commitSnapshot);

    Flux<CommitSnapshot> insert(Collection<CommitSnapshot> commitSnapshots);

    Flux<CommitSnapshot> mapToPulls(Collection<CommitSnapshot> commitSnapshots);

    Mono<CommitSnapshot> findById(CommitSnapshot.Id id);

    Flux<CommitSnapshot> findById(Collection<CommitSnapshot.Id> ids);
}
