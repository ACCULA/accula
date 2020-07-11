package org.accula.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

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
    GithubApiCommitSnapshot head;
    GithubApiCommitSnapshot base;
    GithubApiUser user;
    Integer number;
    String title;
    State state;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;

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
