package org.accula.api.handlers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true, access = PRIVATE)
public class ProjectDto {
    Long id;
    String repoOwner;
    String repoName;
    String repoDescription;
    String repoOwnerAvatar;
    String repoUrl;
    Integer repoOpenPullCount;
    Long creatorId;
    List<Long> adminIds;
}
