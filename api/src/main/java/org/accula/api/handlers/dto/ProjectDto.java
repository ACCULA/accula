package org.accula.api.handlers.dto;

import lombok.Builder;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
public class ProjectDto {
    Long id;
    String repoOwner;
    String repoName;
    String repoDescription;
    String repoOwnerAvatar;
    String repoUrl;
    Integer repoOpenPullCount;
    Long creatorId;
    @Builder.Default
    Long[] admins = new Long[0];
}
