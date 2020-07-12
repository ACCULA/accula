package org.accula.api.db.repo;

import org.accula.api.db.model.Pull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Anton Lamtev
 */
public interface PullRepo {
    Mono<Pull> upsert(Pull pull);

    Flux<Pull> upsert(Collection<Pull> pulls);

    Mono<Pull> findById(Long id);

    Flux<Pull> findById(Collection<Long> ids);

    Mono<Pull> findByNumber(Long projectId, Integer number);

    Flux<Pull> findUpdatedEarlierThan(Long projectId, Integer number);

    Flux<Pull> findByProjectId(Long projectId);

    Flux<Integer> numbersByIds(Collection<Long> ids);
}
