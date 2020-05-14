package org.accula.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebHookModel {
    @JsonProperty("pull_request")
    private GitPullRequest pullRequest;
}
