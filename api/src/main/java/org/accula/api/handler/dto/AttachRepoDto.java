package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
public final class AttachRepoDto {
    private AttachRepoDto() {
    }

    @JsonAutoDetect(fieldVisibility = ANY)
    @Value
    public static class ByUrl implements InputDto {
        String url;
    }

    @JsonAutoDetect(fieldVisibility = ANY)
    @Value
    public static class ByInfo implements InputDto {
        RepoShortDto info;
    }
}
