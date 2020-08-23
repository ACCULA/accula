package org.accula.api.handlers.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * @author Anton Lamtev
 */
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
    CloneDetectionState cloneDetectionState;
    @Builder.Default
    Integer cloneCount = 0;
    GithubUserDto author;
    List<ShortPullDto> previousPulls;

    @Value
    public static class Marker {
        String url;
        String label;
    }

    public enum CloneDetectionState {
        NOT_YET_RUN,
        PENDING,
        RUNNING,
        FINISHED,
        ;
    }
}
