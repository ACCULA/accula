package org.accula.api.db;

import org.accula.api.db.model.Project;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectsRepository extends ReactiveCrudRepository<Project, Long> {
}
