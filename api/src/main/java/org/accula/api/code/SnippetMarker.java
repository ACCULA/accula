package org.accula.api.code;

import org.accula.api.code.lines.LineRange;

/**
 * @author Anton Lamtev
 */
public record SnippetMarker(String filename, LineRange lines) {
}
