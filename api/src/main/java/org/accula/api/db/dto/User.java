package org.accula.api.db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
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
    private Long id;
    @Nullable
    private String firstName;
    @Nullable
    private String lastName;
    private Long githubId;
    private String githubAccessToken;

    public static User of(final Long githubId, final String githubAccessToken) {
        return new User(null, null, null, githubId, githubAccessToken);
    }

    public static User of(final Long id, final Long githubId, final String githubAccessToken) {
        return new User(id, null, null, githubId, githubAccessToken);
    }
}
