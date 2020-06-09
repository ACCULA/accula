package org.accula.api.handlers.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

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
    @Builder.Default
    CloneStatus status = CloneStatus.FINISHED;
    @Builder.Default
    Integer cloneCount = 0;
    GithubUserDto author;

    @Value
    public static class Marker {
        String url;
        String label;
    }

    public enum CloneStatus {
        PENDING,
        FINISHED,
        ;
    }
}