package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
public record CloneDto(Long id, Long projectId, FlatCodeSnippet target, FlatCodeSnippet source) {
    @JsonAutoDetect(fieldVisibility = ANY)
    @Builder
    public record FlatCodeSnippet(
        Integer pullNumber,
        String owner,
        String repo,
        String sha,
        String file,
        Integer fromLine,
        Integer toLine,
        String content,
        String pullUrl,
        String commitUrl,
        String fileUrl
    ) {
    }
}
