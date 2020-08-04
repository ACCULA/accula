package org.accula.api.code;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class SnippetMarker {
    String filename;
    int fromLine;
    int toLine;
}
