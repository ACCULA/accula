package org.accula.api.db.repo;

import org.accula.api.db.model.Commit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
public interface CommitRepo {
    Mono<Commit> upsert(Commit commit);

    Flux<Commit> upsert(Collection<Commit> commits);

    Mono<Commit> findBySha(String sha);

    Flux<Commit> findBySha(Collection<String> shas);
}
