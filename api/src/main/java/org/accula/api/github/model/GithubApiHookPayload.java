package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubApiHookPayload {
    GithubApiRepo repository;
    @JsonProperty("pull_request")
    GithubApiPull pull;
}
