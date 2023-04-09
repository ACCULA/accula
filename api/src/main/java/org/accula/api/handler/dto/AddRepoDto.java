package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
public final class AddRepoDto {
    private AddRepoDto() {
    }

    @JsonAutoDetect(fieldVisibility = ANY)
    public record ByUrl(String url) implements InputDto {
    }

    @JsonAutoDetect(fieldVisibility = ANY)
    public record ByInfo(RepoShortDto info) implements InputDto {
    }
}
