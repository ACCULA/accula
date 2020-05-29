package org.accula.api.handlers.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
public class GetPullResponseBody implements ResponseBody {
    final Long projectId;
    final Integer number;
    final String url;
    final String title;
    final PullRef source;
    final PullRef target;
    final PullAuthor author;
    final Boolean open;
    final Instant createdAt;
    final Instant updatedAt;
    final PullStatus status;
    final Integer cloneCount;
    //An empty array
    final int[] previousPulls = new int[0];

    public enum PullStatus {
        PENDING,
        FINISHED,
        ;
    }

    @Value
    public static class PullRef {
        String url;
        String label;
    }

    @Value
    public static class PullAuthor {
        String login;
        String avatar;
        String url;
    }
}
