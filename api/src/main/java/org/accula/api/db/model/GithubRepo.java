package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GithubRepo {
    @EqualsAndHashCode.Include
    Long id;
    String name;
    Boolean isPrivate;
    String description;
    GithubUser owner;

    public Identity identity() {
        return Identity.of(owner.login(), name);
    }

    @Value(staticConstructor = "of")
    public static class Identity {
        String owner;
        String name;
    }
}
