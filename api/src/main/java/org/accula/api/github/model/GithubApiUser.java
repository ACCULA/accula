package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * @author Anton Lamtev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class GithubApiUser {
    private Long id;
    private String login;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @JsonProperty("html_url")
    private String htmlUrl;
    @Nullable
    private String name;
    private Type type;

    public enum Type {
        USER,
        ORGANIZATION,
        BOT,
        ;

        @JsonValue
        public String value() {
            final var name = name();
            return name.charAt(0) + name.substring(1).toLowerCase(Locale.US);
        }
    }
}
