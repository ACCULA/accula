package org.accula.api.code;

import org.accula.api.db.model.CommitSnapshot;
import org.eclipse.jgit.lib.Repository;
import reactor.core.publisher.Mono;

/**
 * @author Vadim Dyachkov
 */
public interface RepositoryProvider {
    Mono<Repository> getRepository(CommitSnapshot snapshot);
}
