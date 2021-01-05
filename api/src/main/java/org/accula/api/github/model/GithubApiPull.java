package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Locale;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Value
@NoArgsConstructor(force = true, access = PRIVATE)
@AllArgsConstructor
public class GithubApiPull {
    Long id;
    @JsonProperty("html_url")
    String htmlUrl;
    GithubApiSnapshot head;
    GithubApiSnapshot base;
    GithubApiUser user;
    Integer number;
    String title;
    State state;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;
    @Nullable
    @JsonProperty("merged_at")
    Instant mergedAt;

    public boolean isValid() {
        return head.getRepo() != null && head.getUser() != null && !head.getUser().didDeleteAccount();
    }

    public enum State {
        ALL,
        OPEN,
        CLOSED,
        ;

        @JsonValue
        public String value() {
            return name().toLowerCase(Locale.US);
        }
    }
}
