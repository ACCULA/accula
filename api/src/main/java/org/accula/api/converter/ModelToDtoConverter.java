package org.accula.api.converter;

import org.accula.api.db.model.Project;
import org.accula.api.handlers.dto.ProjectDto;
import org.springframework.stereotype.Component;

/**
 * @author Anton Lamtev
 */
@Component
public final class ModelToDtoConverter {
    private static final String GITHUB_REPO_FORMAT = "https://github.com/%s/%s";

    public ProjectDto convert(final Project project) {
        return convert(project, project.getOpenPullCount());
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
}
