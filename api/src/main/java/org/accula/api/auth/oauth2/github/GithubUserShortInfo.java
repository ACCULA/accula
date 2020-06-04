package org.accula.api.auth.oauth2.github;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubUserShortInfo {
    Long id;
    String login;
    @Nullable
    String name;
    String avatar;
}
