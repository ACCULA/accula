package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Instant;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pull {
    @EqualsAndHashCode.Include
    Long id;
    Integer number;
    String title;
    boolean open;
    Instant createdAt;
    Instant updatedAt;
    CommitSnapshot head;
    CommitSnapshot base;
    GithubUser author;
    Long projectId;
    CloneDetectionState cloneDetectionState;

    public enum CloneDetectionState {
        NOT_YET_RUN,
        PENDING,
        RUNNING,
        FINISHED,
        ;

        public static String POSTGRES_NAME = "clone_detection_state_e";
    }
}
