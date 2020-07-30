package org.accula.api.code.git;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class Snippet implements Identifiable {
    public File file;
    public int fromLine;
    public int toLine;

    @Override
    public String getId() {
        return file.id;
    }
}
