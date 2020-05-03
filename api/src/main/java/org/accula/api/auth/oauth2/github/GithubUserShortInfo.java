package org.accula.api.auth.oauth2.github;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubUserShortInfo {
    Long id;
    String login;
    String name;
}
