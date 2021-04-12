package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * @author Anton Lamtev
 */
@Builder(toBuilder = true)
@With
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
    @Builder.Default
    List<GithubUser> assignees = List.of();
    @Nullable
    Long primaryProjectId;
}
