package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CommitSnapshot {
    @EqualsAndHashCode.Include
    Commit commit;
    String branch;
    @Nullable
    Long pullId;
    @EqualsAndHashCode.Include
    GithubRepo repo;

    public Id getId() {
        return new Id(commit.getSha(), repo.getId());
    }

    @Override
    public String toString() {
        return repo.getOwner().getLogin() + "/" + repo.getName() + "/" + commit.getSha();
    }

    @SuppressWarnings("PMD.ShortClassName")
    @Value
    public static class Id {
        String sha;
        Long repoId;
    }
}
