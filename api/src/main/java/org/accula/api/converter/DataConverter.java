package org.accula.api.converter;

import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.github.model.GithubApiRepo;
import org.accula.api.github.model.GithubApiUser;
import org.accula.api.handlers.dto.ProjectDto;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Anton Lamtev
 */
@Component
public final class DataConverter {
    private static final String EMPTY = "";
    private static final String GITHUB_REPO_FORMAT = "https://github.com/%s/%s";

    public GithubRepo convert(final GithubApiRepo apiRepo) {
        return new GithubRepo(
                apiRepo.getId(),
                apiRepo.getName(),
                orEmpty(apiRepo.getDescription()),
                convert(apiRepo.getOwner())
        );
    }

    public GithubUser convert(final GithubApiUser apiUser) {
        return new GithubUser(
                apiUser.getId(),
                apiUser.getLogin(),
                orEmpty(apiUser.getName()),
                apiUser.getAvatarUrl(),
                apiUser.getType() == GithubApiUser.Type.ORGANIZATION
        );
    }

    public GithubUser convert(final Map<String, Object> attributes) {
        final var id = ((Number) attributes.get("id")).longValue();
        final var login = (String) attributes.get("login");
        final var name = (String) attributes.getOrDefault("name", "");
        final var avatar = (String) attributes.get("avatar_url");
        return new GithubUser(id, login, name, avatar, false);
    }

    public ProjectDto convert(final Project project, final int openPullCount) {
        return ProjectDto.builder()
                .id(project.getId())
                .repoOwner(project.getGithubRepo().getOwner().getLogin())
                .repoName(project.getGithubRepo().getName())
                .repoDescription(project.getGithubRepo().getDescription())
                .repoOwnerAvatar(project.getGithubRepo().getOwner().getAvatar())
                .repoUrl(String.format(GITHUB_REPO_FORMAT, project.getGithubRepo().getOwner().getLogin(), project.getGithubRepo().getName()))
                .repoOpenPullCount(openPullCount)
                .creatorId(project.getCreator().getId())
                .admins(new Long[0])
                .build();
    }

    private static String orEmpty(@Nullable final String s) {
        return s != null ? s : EMPTY;
    }
}
