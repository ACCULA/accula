package org.accula.api.code;

import org.eclipse.jgit.lib.Repository;
import reactor.core.publisher.Mono;

/**
 * @author Vadim Dyachkov
 */
public interface RepositoryProvider {
    Mono<Repository> getRepository(String owner, String repo);
}
