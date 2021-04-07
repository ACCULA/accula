package org.accula.api.code.git;

import lombok.Value;
import org.accula.api.code.lines.LineSet;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class GitFileChanges implements Identifiable {
    private static final GitFileChanges EMPTY = GitFileChanges.of(GitFile.devNull(), LineSet.empty());

    GitFile file;
    LineSet changedLines;

    public static GitFileChanges empty() {
        return EMPTY;
    }

    @Override
    public String id() {
        return file.id();
    }
}
