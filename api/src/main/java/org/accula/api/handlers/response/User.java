package org.accula.api.handlers.response;

import lombok.Value;

import static java.util.Objects.requireNonNull;

/**
 * @author Anton Lamtev
 */
@Value
public class User implements ResponseBody {
    Long id;
    String login;
    String name;

    public static User from(final org.accula.api.db.dto.User user) {
        return new User(requireNonNull(user.getId()), user.getGithubLogin(), user.getName());
    }
}
