package org.accula.api.code.git;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class Snippet implements Identifiable {
    File file;
    int fromLine;
    int toLine;

    @Override
    public String getId() {
        return file.getId();
    }
}