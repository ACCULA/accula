package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @author Anton Lamtev
 */
@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
@With
@Builder
@Value
public class GithubApiPushHookPayload {
    String ref;
    String before;
    String after;
    @JsonProperty("repository")
    GithubApiRepo repo;
}
