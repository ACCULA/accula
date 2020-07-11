package org.accula.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Value
@NoArgsConstructor(force = true, access = PRIVATE)
@AllArgsConstructor
public class GithubApiUser {
    Long id;
    String login;
    @JsonProperty("avatar_url")
    String avatarUrl;
    @JsonProperty("html_url")
    String htmlUrl;
    @Nullable
    String name;
    Type type;

    public boolean didDeleteAccount() {
        // Github sets the Ghost user instead of any user that deleted its account
        return "ghost".equals(login) || Long.valueOf(10137L).equals(id);
    }

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
