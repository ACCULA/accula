package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Value
@NoArgsConstructor(force = true, access = PRIVATE)
@AllArgsConstructor
public class GithubApiHookPayload {
    @JsonProperty("repository")
    GithubApiRepo repo;
    @JsonProperty("pull_request")
    GithubApiPull pull;
}
