package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import java.util.Locale;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubApiHookPayload {
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
