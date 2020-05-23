package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.time.Instant;
import java.util.Locale;

/**
 * @author Anton Lamtev
 */
@Data
public final class GithubPull {
    @JsonProperty("html_url")
    private String htmlUrl;
    private Attendee head;
    private Attendee base;
    private GithubUser user;
    private Integer number;
    private String title;
    private State state;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;

    public enum State {
        OPEN,
        CLOSED,
        ;

        @JsonValue
        public String value() {
            return name().toLowerCase(Locale.US);
        }
    }

    @Data
    public static final class Attendee {
        private String label;
        private GithubRepo repo;
    }
}
