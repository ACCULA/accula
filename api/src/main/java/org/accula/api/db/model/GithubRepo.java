package org.accula.api.db.model;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubRepo {
    Long id;
    String name;
    GithubUser owner;
    String description;
}
