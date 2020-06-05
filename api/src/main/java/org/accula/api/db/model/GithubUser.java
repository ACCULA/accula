package org.accula.api.db.model;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubUser {
    Long id;
    String login;
    String name;
    String avatar;
    boolean organization;
}
