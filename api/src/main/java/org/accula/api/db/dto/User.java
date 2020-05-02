package org.accula.api.db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Anton Lamtev
 */
@Table("users")
@Data
@AllArgsConstructor
public class User {
    @Id
    @Nullable
    Long id;
    @Nullable
    String firstName;
    @Nullable
    String lastName;
    Long githubId;
    String githubAccessToken;

    @NotNull
    public static User of(@NotNull final Long githubId, @NotNull final String githubAccessToken) {
        return new User(null, null, null, githubId, githubAccessToken);
    }

    @NotNull
    public static User of(@NotNull final Long id, @NotNull final Long githubId, @NotNull final String githubAccessToken) {
        return new User(id, null, null, githubId, githubAccessToken);
    }
}
