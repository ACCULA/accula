package org.accula.api.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

/**
 * @author Anton Lamtev
 */
@Table("users")
@Data
@AllArgsConstructor
public class UserOld {
    @Id
    @Nullable
    private Long id;
    @Nullable
    private String name;
    private Long githubId;
    private String githubLogin;
    @Nullable
    private String githubAccessToken;

    public boolean shouldUpdateLoginOrAccessToken(final String login, final String accessToken) {
        return !githubLogin.equals(login) || !Objects.equals(githubAccessToken, accessToken);
    }

    public static UserOld of(@Nullable final String name, final Long githubId, final String githubLogin, final String githubAccessToken) {
        return new UserOld(null, name, githubId, githubLogin, githubAccessToken);
    }
}