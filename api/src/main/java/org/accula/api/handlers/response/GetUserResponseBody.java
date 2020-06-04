package org.accula.api.handlers.response;

import lombok.Value;
import org.accula.api.db.model.UserOld;

import static java.util.Objects.requireNonNull;

/**
 * @author Anton Lamtev
 */
@Value
public class GetUserResponseBody implements ResponseBody {
    Long id;
    String login;
    String name;

    public static GetUserResponseBody from(final UserOld user) {
        return new GetUserResponseBody(requireNonNull(user.getId()), user.getGithubLogin(), user.getName());
    }
}
