package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
public record ProjectDto(
    Long id,
    State state,
    String repoOwner,
    String repoName,
    String repoDescription,
    String repoOwnerAvatar,
    String repoUrl,
    Integer repoOpenPullCount,
    Long creatorId,
    List<Long> adminIds,
    List<RepoShortDto> secondaryRepos
) {
    public enum State {
        CONFIGURING,
        CONFIGURED,
    }
}
