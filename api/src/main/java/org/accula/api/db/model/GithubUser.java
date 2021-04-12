package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GithubUser {
    @EqualsAndHashCode.Include
    Long id;
    String login;
    @Nullable
    String name;
    String avatar;
    boolean isOrganization;
}
