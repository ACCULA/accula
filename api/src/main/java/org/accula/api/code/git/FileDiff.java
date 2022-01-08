package org.accula.api.code.git;

import lombok.Value;
import org.accula.api.code.lines.LineSet;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class FileDiff implements Identifiable {
    private static final FileDiff EMPTY = FileDiff.of(GitFile.devNull(), LineSet.empty());

    GitFile file;
    LineSet changedLines;

    public static FileDiff empty() {
        return EMPTY;
    }

    @Override
    public String id() {
        return file.id();
    }
}
