package org.accula.api.db;

import org.accula.api.db.model.Project;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
public interface ProjectRepository extends ReactiveCrudRepository<Project, Long> {
    @Query("SELECT NOT exists(SELECT 0 FROM project WHERE repo_owner = :repoOwner AND repo_name = :repoName)")
    Mono<Boolean> notExistsByRepoOwnerAndRepoName(String repoOwner, String repoName);

    Mono<Boolean> deleteByIdAndCreatorId(Long id, Long creatorId);

    Mono<Project> findByRepoOwnerAndRepoName(String repoOwner, String repoName);

    //@formatter:off
    @Query("UPDATE project " +
           "SET admins = :admins " +
           "WHERE id = :id AND creator_id = :creatorId")
    Mono<Void> setAdmins(Long id, Long[] admins, Long creatorId);
    //@formatter:on
}
