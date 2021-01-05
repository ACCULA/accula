package org.accula.api.db.repo;

import org.accula.api.db.model.Commit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface CommitRepo {
    default Mono<Commit> insert(final Commit commit) {
        return insert(List.of(commit)).next();
    }

    Flux<Commit> insert(Collection<Commit> commits);
}
