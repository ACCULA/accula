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
    Long projectId;//+
    Integer number;//+
    String url; // generate
    String title; //+
    PullRef head; //+
    PullRef base; //+
    PullAuthor author; //+
    Boolean open; //+
    Instant createdAt;//+
    Instant updatedAt;//+
    PullStatus status;// stub
    Integer cloneCount;// SELECT count(*)
    //An empty array
    int[] previousPulls = new int[0];

    public enum PullStatus {
        PENDING,
        FINISHED,
        ;
    }

    @Value
    public static class PullRef {
        //String ref; // (branch name)
        //String repo
        String url; //ref + url
        String label; // = Repo:ref
    }

    @Value
    public static class PullAuthor {
        //Long githubId
        String login;
        String avatar;
        String url; // generate
    }
}
