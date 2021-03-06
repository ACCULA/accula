package org.accula.api.db.repo;

import org.accula.api.db.model.Pull;
import org.accula.api.db.model.PullSnapshots;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface PullRepo {
    default Mono<Pull> upsert(final Pull pull) {
        return upsert(List.of(pull)).next();
    }

    Flux<Pull> upsert(Collection<Pull> pulls);

    default Mono<Pull> findById(final Long id) {
        return findById(List.of(id)).next();
    }

    Flux<Pull> findById(Collection<Long> ids);

    Mono<Pull> findByNumber(Long projectId, Integer number);

    Flux<Pull> findPrevious(Long projectId, Integer number, Long authorId);

    Flux<Pull> findByProjectId(Long projectId);

    Flux<Pull> findByProjectIdIncludingAssignees(Long projectId);

    Flux<Pull> findByProjectIdIncludingSecondaryRepos(Long projectId);

    default Publisher<Void> mapSnapshots(final PullSnapshots pullSnapshots) {
        return mapSnapshots(List.of(pullSnapshots));
    }

    Publisher<Void> mapSnapshots(final Iterable<PullSnapshots> pullSnapshots);
}
