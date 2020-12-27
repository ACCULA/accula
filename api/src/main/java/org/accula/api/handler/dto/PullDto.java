package org.accula.api.handler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true, access = PRIVATE)
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

    @Value
    @AllArgsConstructor
    @NoArgsConstructor(force = true, access = PRIVATE)
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
