package org.accula.api.db;

import org.accula.api.db.model.Commit;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * @author Vadim Dyachkov
 */
public interface CommitRepository extends ReactiveCrudRepository<Commit, Long> {
}
