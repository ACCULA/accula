package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @author Anton Lamtev
 */
@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
@Value
public class GithubApiRepo {
    Long id;
    @JsonProperty("html_url")
    String htmlUrl;
    String name;
    @JsonProperty("private")
    Boolean isPrivate;
    @Nullable
    String description;
    GithubApiUser owner;
}
