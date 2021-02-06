package org.accula.api.code.git;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class Snippet implements Identifiable {
    GitFile file;
    int fromLine;
    int toLine;

    @Override
    public String id() {
        return file.id();
    }
}
