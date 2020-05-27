package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GithubWebhook {
    @JsonProperty("pull_request")
    private GithubPull pullRequest;
}
