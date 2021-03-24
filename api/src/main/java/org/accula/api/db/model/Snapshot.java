package org.accula.api.db.model;

import lombok.Builder;
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
public class Snapshot {
    Commit commit;
    String branch;
    GithubRepo repo;
    @Nullable
    PullInfo pullInfo;

    public Id id() {
        return new Id(commit.sha(), repo.id(), branch);
    }

    public String sha() {
        return commit.sha();
    }

    public Snapshot withPull(final Pull pull) {
        return withPullInfo(PullInfo.of(pull.id(), pull.number()));
    }

    @Override
    public String toString() {
        return (pullInfo != null ? "#" + pullInfo.number + "@" : "") + repo.owner().login() + "/" + repo.name() + "/" + commit.sha();
    }

    @SuppressWarnings("PMD.ShortClassName")
    @Value
    public static class Id {
        String sha;
        Long repoId;
        String branch;
    }

    @Value(staticConstructor = "of")
    public static class PullInfo {
        Long id;
        Integer number;
    }
}
