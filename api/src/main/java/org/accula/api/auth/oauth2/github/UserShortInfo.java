package org.accula.api.auth.oauth2.github;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class UserShortInfo {
    Long id;
    String login;
    String name;
}
