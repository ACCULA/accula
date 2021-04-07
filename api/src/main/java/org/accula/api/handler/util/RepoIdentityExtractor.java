package org.accula.api.handler.util;

import org.accula.api.db.model.GithubRepo;
import org.accula.api.handler.dto.AddRepoDto;
import org.accula.api.handler.dto.CreateProjectDto;
import org.accula.api.handler.exception.ProjectsHandlerException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Anton Lamtev
 */
public final class RepoIdentityExtractor {
    private RepoIdentityExtractor() {
    }

    public static GithubRepo.Identity repoIdentity(final CreateProjectDto requestBody) {
        return repoIdentity(requestBody.githubRepoUrl());
    }

    public static GithubRepo.Identity repoIdentity(final AddRepoDto.ByUrl requestBody) {
        return repoIdentity(requestBody.url());
    }

    public static GithubRepo.Identity repoIdentity(final AddRepoDto.ByInfo requestBody) {
        return GithubRepo.Identity.of(requestBody.info().owner(), requestBody.info().name());
    }

    private static GithubRepo.Identity repoIdentity(final String url) {
        final var pathSegments = UriComponentsBuilder
            .fromUriString(url)
            .build()
            .getPathSegments();
        if (pathSegments.size() != 2) {
            throw ProjectsHandlerException.invalidUrl(url);
        }
        return GithubRepo.Identity.of(pathSegments.get(0), pathSegments.get(1));
    }
}
