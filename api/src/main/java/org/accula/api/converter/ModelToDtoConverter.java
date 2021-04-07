package org.accula.api.converter;

import com.google.common.base.Preconditions;
import org.accula.api.code.FileEntity;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.GithubRepo;
import org.accula.api.db.model.GithubUser;
import org.accula.api.db.model.Project;
import org.accula.api.db.model.Pull;
import org.accula.api.db.model.Snapshot;
import org.accula.api.db.model.User;
import org.accula.api.handler.dto.CloneDto;
import org.accula.api.handler.dto.GithubUserDto;
import org.accula.api.handler.dto.ProjectConfDto;
import org.accula.api.handler.dto.ProjectDto;
import org.accula.api.handler.dto.PullDto;
import org.accula.api.handler.dto.RepoShortDto;
import org.accula.api.handler.dto.ShortPullDto;
import org.accula.api.handler.dto.UserDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

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
                .secondaryRepos(convert(project.secondaryRepos()))
                .build();
    }

    public static ProjectDto.State convert(final Project.State state) {
        return switch (state) {
            case CONFIGURING -> ProjectDto.State.CREATING;
            case CONFIGURED -> ProjectDto.State.CREATED;
        };
    }

    public static List<RepoShortDto> convert(final Collection<GithubRepo> repos) {
        if (repos.isEmpty()) {
            return List.of();
        }
        return repos
            .stream()
            .map(ModelToDtoConverter::convert)
            .collect(Collectors.toList());
    }

    public static RepoShortDto convert(final GithubRepo repo) {
        return RepoShortDto.builder()
            .id(repo.id())
            .owner(repo.owner().login())
            .name(repo.name())
            .build();
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
                .projectId(Objects.requireNonNull(pull.primaryProjectId(), "Pull.primaryProjectId MUST NOT be null"))
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
            return List.of();
        }
        return pulls
                .stream()
                .map(ModelToDtoConverter::convertShort)
                .collect(Collectors.toList());
    }

    public static ShortPullDto convertShort(final Pull pull) {
        return ShortPullDto.builder()
                .projectId(Objects.requireNonNull(pull.primaryProjectId(), "Pull.primaryProjectId MUST NOT be null"))
                .number(pull.number())
                .url(pullUrl(pull))
                .title(pull.title())
                .createdAt(pull.createdAt())
                .updatedAt(pull.updatedAt())
                .open(pull.isOpen())
                .author(convert(pull.author()))
                .build();
    }

    public static CloneDto convert(final Clone clone,
                                   final Long projectId,
                                   final FileEntity<Snapshot> targetFile,
                                   final FileEntity<Snapshot> sourceFile) {
        return CloneDto.builder()
                .id(clone.id())
                .projectId(projectId)
                .target(convert(clone.target(), targetFile))
                .source(convert(clone.source(), sourceFile))
                .build();
    }

    private static CloneDto.FlatCodeSnippet convert(final Clone.Snippet snippet,
                                                    final FileEntity<Snapshot> file) {
        final var snapshot = snippet.snapshot();
        final var pullInfo = Objects.requireNonNull(snapshot.pullInfo(), "PullInfo MUST NOT be null");
        final var fileContent = Objects.requireNonNull(file.content(), "FileEntity content MUST NOT be null").getBytes(UTF_8);

        Preconditions.checkArgument(snapshot.equals(file.ref()));
        Preconditions.checkArgument(snippet.file().equals(file.name()));

        return CloneDto.FlatCodeSnippet.builder()
                .pullNumber(pullInfo.number())
                .owner(snapshot.repo().owner().login())
                .repo(snapshot.repo().name())
                .sha(snapshot.sha())
                .file(snippet.file())
                .fromLine(snippet.fromLine())
                .toLine(snippet.toLine())
                .content(Base64.getEncoder().encodeToString(fileContent))
                .build();
    }

    private static String pullUrl(final Pull pull) {
        final var repo = pull.base().repo();
        return String.format(GITHUB_PULL_URL_FORMAT, repo.owner().login(), repo.name(), pull.number().toString());
    }
}
