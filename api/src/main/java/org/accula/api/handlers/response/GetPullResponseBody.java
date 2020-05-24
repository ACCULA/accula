package org.accula.api.handlers.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author Anton Lamtev
 */
@Builder
public final class GetPullResponseBody implements ResponseBody {
    private final Long projectId;
    private final Integer number;
    private final String url;
    private final String title;
    private final PullRef source;
    private final PullRef target;
    private final PullAuthor author;
    private final Boolean open;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final PullStatus status;
    private final Integer cloneCount;
    //An empty array
    private final int[] previousPulls = new int[0];

    public enum PullStatus {
        PENDING,
        FINISHED,
        ;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class PullRef {
        private String url;
        private String label;
    }

    @Data
    @AllArgsConstructor
    public static final class PullAuthor {
        private String login;
        private String avatar;
        private String url;
    }
}
