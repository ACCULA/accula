package org.accula.api.converter;

import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.model.User;
import org.accula.api.handlers.dto.GithubUserDto;
import org.accula.api.handlers.dto.ProjectConfDto;
import org.accula.api.handlers.dto.ProjectDto;
import org.accula.api.handlers.dto.PullDto;
import org.accula.api.handlers.dto.ShortPullDto;
import org.accula.api.handlers.dto.UserDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
public final class ModelToDtoConverter {
    private static final String GITHUB_USER_URL_FORMAT = "https://github.com/%s";
    private static final String GITHUB_REPO_URL_FORMAT = GITHUB_USER_URL_FORMAT + "/%s";
    private static final String GITHUB_PULL_URL_FORMAT = GITHUB_REPO_URL_FORMAT + "/pull/%s";

    private ModelToDtoConverter() {
    }

    public static ProjectDto convert(final Project project) {
        return convert(project, project.getOpenPullCount());
    }

    public static ProjectDto convert(final Project project, final int openPullCount) {
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
                .adminIds(project.getAdminIds())
                .build();
    }

    public static ProjectConfDto convert(final Project.Conf conf) {
        return ProjectConfDto.builder()
                .admins(conf.getAdminIds())
                .cloneMinLineCount(conf.getCloneMinLineCount())
                .build();
    }

    public static UserDto convert(final User user) {
        return new UserDto(user.getId(), user.getGithubUser().getLogin(), user.getGithubUser().getName(), user.getGithubUser().getAvatar());
    }

    public static GithubUserDto convert(final GithubUser user) {
        return new GithubUserDto(user.getLogin(), user.getAvatar(), String.format(GITHUB_USER_URL_FORMAT, user.getLogin()));
    }

    public static PullDto convert(final Pull pull, final List<Pull> previousPulls) {
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
                .previousPulls(convertShort(previousPulls))
                .build();
    }

    public static PullDto.Marker convert(final Snapshot snapshot) {
        return new PullDto.Marker(
                String.format(
                        GITHUB_REPO_URL_FORMAT + "/tree/%s",
                        snapshot.getRepo().getOwner().getLogin(),
                        snapshot.getRepo().getName(),
                        URLEncoder.encode(snapshot.getBranch(), StandardCharsets.UTF_8)
                ),
                String.format("%s:%s", snapshot.getRepo().getOwner().getLogin(), snapshot.getBranch())
        );
    }

    public static List<ShortPullDto> convertShort(final List<Pull> pulls) {
        if (pulls.isEmpty()) {
            return Collections.emptyList();
        }
        return pulls
                .stream()
                .map(ModelToDtoConverter::convertShort)
                .collect(Collectors.toList());
    }

    public static ShortPullDto convertShort(final Pull pull) {
        return ShortPullDto.builder()
                .projectId(pull.getProjectId())
                .number(pull.getNumber())
                .url(pullUrl(pull))
                .title(pull.getTitle())
                .open(pull.isOpen())
                .author(convert(pull.getAuthor()))
                .build();
    }

    private static String pullUrl(final Pull pull) {
        final var repo = pull.getBase().getRepo();
        return String.format(GITHUB_PULL_URL_FORMAT, repo.getOwner().getLogin(), repo.getName(), pull.getNumber().toString());
    }
}
