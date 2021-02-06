package org.accula.api.converter;

import org.accula.api.code.git.GitCommit;
import org.accula.api.db.model.Commit;

/**
 * @author Anton Lamtev
 */
public final class GitToModelConverter {
    private GitToModelConverter() {
    }

    public static Commit convert(final GitCommit commit) {
        return Commit.builder()
                .sha(commit.sha())
                .isMerge(commit.isMerge())
                .authorName(commit.authorName())
                .authorEmail(commit.authorEmail())
                .date(commit.date())
                .build();
    }
}
