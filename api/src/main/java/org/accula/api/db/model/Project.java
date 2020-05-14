package org.accula.api.db.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;

/**
 * @author Anton Lamtev
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private static final Long[] ADMINS_EMPTY = new Long[0];

    @Id
    @Nullable
    private Long id;
    private Long creatorId;
    private String repoUrl;
    private String repoName;
    private String repoDescription;
    private Integer repoOpenPullCount;
    private String repoOwner;
    private String repoOwnerAvatar;
    @Builder.Default
    private Long[] admins = ADMINS_EMPTY;

    public static Project of(final Long creatorId, final String repoUrl, final String repoName, final String repoDescription,
                             final Integer repoOpenPullCount, final String repoOwner, final String repoOwnerAvatar) {
        return new Project(null, creatorId, repoUrl, repoName, repoDescription, repoOpenPullCount, repoOwner, repoOwnerAvatar, ADMINS_EMPTY);
    }
}
