package org.accula.api.code;

import org.accula.api.db.model.CommitSnapshot;
import org.eclipse.jgit.lib.Repository;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface RepositoryUpdater {
    Mono<Repository> addAndFetchRemote(CommitSnapshot repoOrigin, CommitSnapshot remote);
}
