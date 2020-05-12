package org.accula.api.handlers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
public final class CreateProjectRequestBody implements RequestBody {
    @NotNull
    @JsonProperty(required = true)
    private final String githubRepoUrl;
}
