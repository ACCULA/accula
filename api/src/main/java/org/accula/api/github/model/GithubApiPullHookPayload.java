package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.Locale;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @author Anton Lamtev
 */
@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
@With
@Builder
@Value
public class GithubApiPullHookPayload {
    Action action;
    @JsonProperty("repository")
    GithubApiRepo repo;
    @JsonProperty("pull_request")
    GithubApiPull pull;

    public enum Action {
        OPENED,
        CLOSED,
        REOPENED,
        SYNCHRONIZE,
        EDITED,
        ASSIGNED,
        UNASSIGNED,
        ;

        @JsonValue
        public String value() {
            return name().toLowerCase(Locale.US);
        }
    }
}
