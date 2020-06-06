package org.accula.api.db.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GithubUser {
    @EqualsAndHashCode.Include
    Long id;
    String login;
    String name;
    String avatar;
    boolean organization;
}
