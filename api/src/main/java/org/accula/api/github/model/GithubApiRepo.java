package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubApiRepo {
    Long id;
    @JsonProperty("html_url")
    String htmlUrl;
    String name;
    @Nullable
    String description;
    GithubApiUser owner;
}
