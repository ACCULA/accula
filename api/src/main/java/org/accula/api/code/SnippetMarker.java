package org.accula.api.code;

import lombok.Value;
import org.accula.api.code.lines.LineRange;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
public class SnippetMarker {
    String filename;
    LineRange lines;
}
