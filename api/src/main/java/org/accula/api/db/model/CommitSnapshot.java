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
    String commitSha;
    String branch;
    @Nullable
    Long pullId;
    @EqualsAndHashCode.Include
    GithubRepo repo;

    public Id getId() {
        return new Id(commitSha, repo.getId());
    }

    @Override
    public String toString() {
        return repo.getOwner().getLogin() + "/" + repo.getName() + "/" + commitSha;
    }

    @SuppressWarnings("PMD.ShortClassName")
    @Value
    public static class Id {
        String sha;
        Long repoId;
    }
}
