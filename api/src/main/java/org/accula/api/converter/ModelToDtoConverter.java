package org.accula.api.converter;

import com.nimbusds.jose.util.StandardCharset;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.db.model.User;
import org.accula.api.handlers.dto.GithubUserDto;
import org.accula.api.handlers.dto.ProjectDto;
import org.accula.api.handlers.dto.PullDto;
import org.accula.api.handlers.dto.UserDto;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;

/**
 * @author Anton Lamtev
 */
@Component
public final class ModelToDtoConverter {
    private static final String GITHUB_USER_URL_FORMAT = "https://github.com/%s";
    private static final String GITHUB_REPO_URL_FORMAT = GITHUB_USER_URL_FORMAT + "/%s";
    private static final String GITHUB_PULL_URL_FORMAT = GITHUB_REPO_URL_FORMAT + "/pull/%s";

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
                .repoUrl(String.format(GITHUB_REPO_URL_FORMAT,
                        project.getGithubRepo().getOwner().getLogin(),
                        project.getGithubRepo().getName()))
                .repoOpenPullCount(openPullCount)
                .creatorId(project.getCreator().getId())
                .build();
    }

    public UserDto convert(final User user) {
        return new UserDto(user.getId(), user.getGithubUser().getLogin(), user.getGithubUser().getName());
    }

    public GithubUserDto convert(final GithubUser user) {
        return new GithubUserDto(user.getLogin(), user.getAvatar(), String.format(GITHUB_USER_URL_FORMAT, user.getLogin()));
    }

    public PullDto convert(final Pull pull) {
        return PullDto.builder()
                .projectId(pull.getProjectId())
                .number(pull.getNumber())
                .url(pullUrl(pull))
                .title(pull.getTitle())
                .head(convert(pull.getHead()))
                .base(convert(pull.getBase()))
                .open(pull.isOpen())
                .createdAt(pull.getCreatedAt())
                .updatedAt(pull.getUpdatedAt())
                .author(convert(pull.getAuthor()))
                .build();
    }

    public PullDto.Marker convert(final CommitSnapshot snapshot) {
        return new PullDto.Marker(
                String.format(
                        GITHUB_REPO_URL_FORMAT + "/tree/%s",
                        snapshot.getRepo().getOwner().getLogin(),
                        snapshot.getRepo().getName(),
                        URLEncoder.encode(snapshot.getBranch(), StandardCharset.UTF_8)
                ),
                String.format("%s:%s", snapshot.getRepo().getOwner().getLogin(), snapshot.getBranch())
        );
    }

    private static String pullUrl(final Pull pull) {
        final var repo = pull.getBase().getRepo();
        return String.format(GITHUB_PULL_URL_FORMAT, repo.getOwner().getLogin(), repo.getName(), pull.getNumber().toString());
    }
}
