package org.accula.api.db;

import org.accula.api.db.model.Commit;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitRepository extends ReactiveCrudRepository<Commit, Long> {
}
