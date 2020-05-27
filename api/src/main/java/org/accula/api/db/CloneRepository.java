package org.accula.api.db;

import org.accula.api.db.model.Clone;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vadim Dyachkov
 */
public interface CloneRepository extends ReactiveCrudRepository<Clone, Long> {
}
