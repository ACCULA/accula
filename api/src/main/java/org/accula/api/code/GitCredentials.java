package org.accula.api.code;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class GitCredentials {
    String login;
    String accessToken;
}
