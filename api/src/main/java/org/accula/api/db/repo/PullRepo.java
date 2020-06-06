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

    Flux<Pull> findByProjectId(Long projectId);
}