package org.accula.api.code;

import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
public interface GitCredentialsProvider {
    Mono<GitCredentials> gitCredentials(Long repoId);
}
