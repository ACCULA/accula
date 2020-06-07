package org.accula.api.handlers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class ProjectDto {
    Long id;
    String repoOwner;
    String repoName;
    String repoDescription;
    String repoOwnerAvatar;
    String repoUrl;
    Integer repoOpenPullCount;
    Long creatorId;
    Long[] admins;
}
