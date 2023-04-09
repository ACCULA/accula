package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Vadim Dyachkov
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
public record ShortPullDto(
    Long projectId,
    Integer number,
    String url,
    String title,
    Instant createdAt,
    Instant updatedAt,
    Boolean open,
    GithubUserDto author
) {
}
