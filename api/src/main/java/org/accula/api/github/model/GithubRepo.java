package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class GithubRepo {
    @JsonProperty("html_url")
    private String htmlUrl;
    private String name;
    @Nullable
    private String description;
    private GithubUser owner;
}
