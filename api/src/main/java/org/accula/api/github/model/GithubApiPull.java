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
    Marker head;
    Marker base;
    GithubApiUser user;
    Integer number;
    String title;
    State state;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;

    public boolean isValid() {
        return head.repo != null && head.user != null && !head.user.didDeleteAccount();
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

    @Value
    @NoArgsConstructor(force = true, access = PRIVATE)
    @AllArgsConstructor
    public static class Marker {
        @Nullable
        String label;
        String ref;
        @Nullable
        GithubApiUser user;
        @Nullable
        GithubApiRepo repo;
        String sha;
    }
}
