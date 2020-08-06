package org.accula.api.db.repo;

import org.accula.api.db.model.CommitSnapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface CommitSnapshotRepo {
    default Mono<CommitSnapshot> insert(CommitSnapshot commitSnapshot) {
        return insert(List.of(commitSnapshot)).next();
    }

    Flux<CommitSnapshot> insert(Collection<CommitSnapshot> commitSnapshots);

    Flux<CommitSnapshot> mapToPulls(Collection<CommitSnapshot> commitSnapshots);

    default Mono<CommitSnapshot> findById(CommitSnapshot.Id id) {
        return findById(List.of(id)).next();
    }

    Flux<CommitSnapshot> findById(Collection<CommitSnapshot.Id> ids);
}
