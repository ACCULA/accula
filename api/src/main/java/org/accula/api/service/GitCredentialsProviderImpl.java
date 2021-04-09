package org.accula.api.service;

import lombok.RequiredArgsConstructor;
import org.accula.api.code.GitCredentials;
import org.accula.api.code.GitCredentialsProvider;
import org.accula.api.db.repo.CurrentUserRepo;
import org.accula.api.db.repo.ProjectRepo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author Anton Lamtev
 */
@Component
@RequiredArgsConstructor
public final class GitCredentialsProviderImpl implements GitCredentialsProvider {
    private final CurrentUserRepo currentUserRepo;
    private final ProjectRepo projectRepo;

    @Override
    public Mono<GitCredentials> gitCredentials(final Long repoId) {
        return currentUserRepo
            .get()
            .switchIfEmpty(projectRepo.findOwnerOfProjectContainingRepo(repoId))
            .switchIfEmpty(Mono.error(new IllegalStateException("No git credentials for repo with id = " + repoId)))
            .map(user -> GitCredentials.of(user.githubUser().login(), user.githubAccessToken()));
    }
}
