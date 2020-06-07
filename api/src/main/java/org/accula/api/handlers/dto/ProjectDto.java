package org.accula.api.handlers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@NoArgsConstructor(force = true, access = PRIVATE)
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
