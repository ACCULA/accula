package org.accula.api.code;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class SnippetMarker {
    public String filename;
    public int fromLine;
    public int toLine;
}
