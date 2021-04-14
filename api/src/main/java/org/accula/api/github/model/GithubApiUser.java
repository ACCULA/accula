package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @author Anton Lamtev
 */
@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
@Value
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
