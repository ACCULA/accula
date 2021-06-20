package org.accula.api.db.model;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

/**
 * @author Anton Lamtev
 */
@With
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @EqualsAndHashCode.Include
    Long id;
    String githubAccessToken;
    GithubUser githubUser;
    Role role;

    public static User noIdentity(final String githubAccessToken, final GithubUser githubUser, final Role role) {
        return new User(-1L, githubAccessToken, githubUser, role);
    }

    public boolean is(final Role role) {
        return this.role == role;
    }

    public enum Role {
        /**
         * Read-only access
         */
        USER,
        /**
         * Is able to create new projects
         */
        ADMIN,
        /**
         * Is able to add new admins
         */
        ROOT,
        ;

        public boolean is(final User user) {
            return this == user.role;
        }
    }
}
