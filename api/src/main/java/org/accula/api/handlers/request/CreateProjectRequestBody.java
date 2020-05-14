package org.accula.api.handlers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Anto Lamtev
 */
@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
public final class CreateProjectRequestBody implements RequestBody {
    @JsonProperty(required = true)
    private String githubRepoUrl;
}
