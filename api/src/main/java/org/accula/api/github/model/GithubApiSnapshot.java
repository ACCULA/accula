package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * @author Anton Lamtev
 */
@JsonInclude(NON_NULL)
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
@Value
public class GithubApiSnapshot {
    @Nullable
    String label;
    String ref;
    @Nullable
    GithubApiUser user;
    @Nullable
    GithubApiRepo repo;
    String sha;
}
