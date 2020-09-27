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
    Snapshot head;
    Snapshot base;
    GithubUser author;
    Long projectId;
}
