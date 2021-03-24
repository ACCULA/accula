package org.accula.api.converter;

import org.accula.api.clone.CodeClone;
import org.accula.api.code.git.GitCommit;
import org.accula.api.db.model.Clone;
import org.accula.api.db.model.Commit;

/**
 * @author Anton Lamtev
 */
public final class CodeToModelConverter {
    private CodeToModelConverter() {
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

    public static Clone convert(final CodeClone clone) {
        return Clone.builder()
                .target(convert(clone.target()))
                .source(convert(clone.source()))
                .build();
    }

    public static Clone.Snippet convert(final CodeClone.Snippet snippet) {
        return Clone.Snippet.builder()
                .snapshot(snippet.snapshot())
                .file(snippet.file())
                .fromLine(snippet.lines().from())
                .toLine(snippet.lines().to())
                .build();
    }
}
