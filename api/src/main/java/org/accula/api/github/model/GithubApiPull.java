package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Locale;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @author Anton Lamtev
 */
@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
@Value
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
    @Nullable
    GithubApiUser assignee;
    GithubApiUser[] assignees;

    public boolean isValid() {
        return head.repo() != null && head.user() != null && !head.user().didDeleteAccount();
    }

    public boolean isNotMerged() {
        return mergedAt == null;
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
