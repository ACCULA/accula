package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
@Value
public class PullDto {
    Long projectId;
    Integer number;
    String url;
    String title;
    Marker head;
    Marker base;
    Boolean open;
    Instant createdAt;
    Instant updatedAt;
    @Builder.Default
    CloneStatus status = CloneStatus.FINISHED;
    @Builder.Default
    Integer cloneCount = 0;
    GithubUserDto author;
    List<ShortPullDto> previousPulls;

    public enum CloneStatus {
        PENDING,
        FINISHED,
    }

    @JsonAutoDetect(fieldVisibility = ANY)
    public record Marker(String url, String label) {
    }
}
