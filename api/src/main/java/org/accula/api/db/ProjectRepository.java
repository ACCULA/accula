package org.accula.api.db;

import org.accula.api.db.model.Project;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface ProjectRepository extends ReactiveCrudRepository<Project, Long> {
    Mono<Project> findById(final Long id);

    <S extends Project> Mono<S> save(final S project);

    @Query("SELECT exists(SELECT 0 FROM project WHERE repo_owner = :repoOwner AND repo_name = :repoName)")
    Mono<Boolean> existsByRepoOwnerAndRepoName(final String repoOwner, final String repoName);

    Mono<Boolean> deleteByIdAndCreatorId(final Long id, final Long creatorId);

    //@formatter:off
    @Query("UPDATE project " +
           "SET admins = :admins " +
           "WHERE id = :id AND creator_id = :creatorId")
    Mono<Void> setAdmins(final Long id, final Long[] admins, final Long creatorId);
    //@formatter:on
}
