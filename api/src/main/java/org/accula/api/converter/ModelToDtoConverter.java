package org.accula.api.converter;

import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.model.User;
import org.accula.api.handler.dto.GithubUserDto;
import org.accula.api.handler.dto.ProjectConfDto;
import org.accula.api.handler.dto.ProjectDto;
import org.accula.api.handler.dto.PullDto;
import org.accula.api.handler.dto.ShortPullDto;
import org.accula.api.handler.dto.UserDto;

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
        return ProjectDto.builder()
                .id(project.id())
                .state(convert(project.state()))
                .repoOwner(project.githubRepo().owner().login())
                .repoName(project.githubRepo().name())
                .repoDescription(project.githubRepo().description())
                .repoOwnerAvatar(project.githubRepo().owner().avatar())
                .repoUrl(String.format(GITHUB_REPO_URL_FORMAT,
                        project.githubRepo().owner().login(),
                        project.githubRepo().name()))
                .repoOpenPullCount(project.openPullCount())
                .creatorId(project.creator().id())
                .adminIds(project.adminIds())
                .build();
    }

    public static ProjectDto.State convert(final Project.State state) {
        return switch (state) {
            case CREATING -> ProjectDto.State.CREATING;
            case CREATED -> ProjectDto.State.CREATED;
        };
    }

    public static ProjectConfDto convert(final Project.Conf conf) {
        return ProjectConfDto.builder()
                .admins(conf.adminIds())
                .cloneMinTokenCount(conf.cloneMinTokenCount())
                .fileMinSimilarityIndex(conf.fileMinSimilarityIndex())
                .excludedFiles(conf.excludedFiles())
                .build();
    }

    public static UserDto convert(final User user) {
        return new UserDto(user.id(), user.githubUser().login(), user.githubUser().name(), user.githubUser().avatar());
    }

    public static GithubUserDto convert(final GithubUser user) {
        return new GithubUserDto(user.login(), user.avatar(), String.format(GITHUB_USER_URL_FORMAT, user.login()));
    }

    public static PullDto convert(final Pull pull, final List<Pull> previousPulls) {
        return PullDto.builder()
                .projectId(pull.projectId())
                .number(pull.number())
                .url(pullUrl(pull))
                .title(pull.title())
                .head(convert(pull.head()))
                .base(convert(pull.base()))
                .open(pull.isOpen())
                .createdAt(pull.createdAt())
                .updatedAt(pull.updatedAt())
                .author(convert(pull.author()))
                .previousPulls(convertShort(previousPulls))
                .build();
    }

    public static PullDto.Marker convert(final Snapshot snapshot) {
        return new PullDto.Marker(
                String.format(
                        GITHUB_REPO_URL_FORMAT + "/tree/%s",
                        snapshot.repo().owner().login(),
                        snapshot.repo().name(),
                        URLEncoder.encode(snapshot.branch(), StandardCharsets.UTF_8)
                ),
                String.format("%s:%s", snapshot.repo().owner().login(), snapshot.branch())
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
                .projectId(pull.projectId())
                .number(pull.number())
                .url(pullUrl(pull))
                .title(pull.title())
                .createdAt(pull.createdAt())
                .updatedAt(pull.updatedAt())
                .open(pull.isOpen())
                .author(convert(pull.author()))
                .build();
    }

    private static String pullUrl(final Pull pull) {
        final var repo = pull.base().repo();
        return String.format(GITHUB_PULL_URL_FORMAT, repo.owner().login(), repo.name(), pull.number().toString());
    }
}
