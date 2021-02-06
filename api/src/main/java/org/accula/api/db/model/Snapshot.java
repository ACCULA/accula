package org.accula.api.db.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Builder
@With
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Snapshot {
    @EqualsAndHashCode.Include
    Commit commit;
    String branch;
    @Nullable
    Long pullId;
    @EqualsAndHashCode.Include
    GithubRepo repo;

    public Id id() {
        return new Id(commit.sha(), repo.id());
    }

    public String sha() {
        return commit.sha();
    }

    @Override
    public String toString() {
        return repo.owner().login() + "/" + repo.name() + "/" + commit.sha();
    }

    @SuppressWarnings("PMD.ShortClassName")
    @Value
    public static class Id {
        String sha;
        Long repoId;
    }
}
