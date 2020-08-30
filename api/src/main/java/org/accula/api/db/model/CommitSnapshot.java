package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Builder
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CommitSnapshot {
    @EqualsAndHashCode.Include
    String sha;
    String branch;
    @Nullable
    Long pullId;
    @EqualsAndHashCode.Include
    GithubRepo repo;

    public Id getId() {
        return new Id(sha, repo.getId());
    }

    @Override
    public String toString() {
        return "#" + pullId + "|" + repo.getOwner().getLogin() + "/" + repo.getName() + "/" + sha;
    }

    @SuppressWarnings("PMD.ShortClassName")
    @Value
    public static class Id {
        String sha;
        Long repoId;
    }
}
