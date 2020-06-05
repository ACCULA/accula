package org.accula.api.db.model;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class User {
    Long id;
    GithubUser githubUser;
    String githubAccessToken;
}
