package org.accula.api.db;

import org.accula.api.db.model.Pull;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PullRepository extends ReactiveCrudRepository<Pull, Long> {
}
