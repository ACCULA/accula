package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.Locale;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Value
@NoArgsConstructor(force = true, access = PRIVATE)
@AllArgsConstructor
public class GithubApiHookPayload {
    Action action;
    @JsonProperty("repository")
    GithubApiRepo repo;
    @JsonProperty("pull_request")
    GithubApiPull pull;

    public enum Action {
        OPENED,
        SYNCHRONIZE,
        EDITED,
        CLOSED,
        ;

        @JsonValue
        public String value() {
            return name().toLowerCase(Locale.US);
        }
    }
}
