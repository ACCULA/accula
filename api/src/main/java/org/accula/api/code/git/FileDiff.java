package org.accula.api.code.git;

import lombok.Value;
import org.accula.api.code.lines.LineSet;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class FileDiff implements Identifiable {
    GitFile file;
    LineSet changedLines;

    @Override
    public String id() {
        return file.id();
    }
}
