package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class GithubHookPayload {
    GithubRepo repository;
    @JsonProperty("pull_request")
    GithubPull pull;
}
