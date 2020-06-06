package org.accula.api.db.model;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GithubRepo {
    @EqualsAndHashCode.Include
    Long id;
    String name;
    String description;
    GithubUser owner;
}
