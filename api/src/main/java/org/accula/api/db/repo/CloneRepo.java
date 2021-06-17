package org.accula.api.db.repo;

import org.accula.api.db.model.Clone;
import org.accula.api.db.model.CloneStatistics;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * @author Anton Lamtev
 */
public interface CloneRepo {
    default Mono<Clone> insert(final Clone clone) {
        return insert(List.of(clone)).next();
    }

    Flux<Clone> insert(Collection<Clone> clones);

    Flux<Clone> findByPullNumber(Long projectId, Integer pullNumber);

    Mono<Void> deleteByPullNumber(Long projectId, Integer pullNumber);

    Flux<CloneStatistics> topPlagiarists(Long projectId);

    Flux<CloneStatistics> topSources(Long projectId);
}
