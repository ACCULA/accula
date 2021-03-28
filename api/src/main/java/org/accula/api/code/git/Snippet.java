package org.accula.api.code.git;

import lombok.Value;
import org.accula.api.code.lines.LineRange;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class Snippet implements Identifiable {
    GitFile file;
    LineRange lines;

    @Override
    public String id() {
        return file.id();
    }
}
