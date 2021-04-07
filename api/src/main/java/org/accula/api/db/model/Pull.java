package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * @author Anton Lamtev
 */
@Builder(toBuilder = true)
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pull {
    @EqualsAndHashCode.Include
    Long id;
    Integer number;
    String title;
    boolean isOpen;
    Instant createdAt;
    Instant updatedAt;
    Snapshot head;
    Snapshot base;
    GithubUser author;
    @Nullable
    Long primaryProjectId;
}
