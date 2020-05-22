package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Anton Lamtev
 */
@Data
public final class GithubPull {
    @JsonProperty("html_url")
    private String htmlUrl;

}
