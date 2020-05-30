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
    Long projectId;
    Integer number;
    String url;
    String title;
    PullRef head;
    PullRef base;
    PullAuthor author;
    Boolean open;
    Instant createdAt;
    Instant updatedAt;
    PullStatus status;
    Integer cloneCount;
    //An empty array
    int[] previousPulls = new int[0];

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
