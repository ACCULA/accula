package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

//TODO: Add some useful urls for better user experience:
// urls to: source and target repos, pulls, commits ...
/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
@Value
public class CloneDto {
    Long id;
    Long projectId;
    FlatCodeSnippet target;
    FlatCodeSnippet source;

    @JsonAutoDetect(fieldVisibility = ANY)
    @Builder
    @Value
    public static class FlatCodeSnippet {
        Integer pullNumber;
        String owner;
        String repo;
        String sha;
        String file;
        Integer fromLine;
        Integer toLine;
        String content;
    }
}
