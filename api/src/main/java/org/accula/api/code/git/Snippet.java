package org.accula.api.code.git;

import org.accula.api.code.lines.LineRange;

/**
 * @author Anton Lamtev
 */
public record Snippet(GitFile file, LineRange lines) implements Identifiable {
    @Override
    public String id() {
        return file.id();
    }
}
