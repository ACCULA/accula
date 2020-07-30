package org.accula.api.code;

import lombok.Value;

@Value(staticConstructor = "of")
public class SnippetMarker {
    String filename;
    int fromLine;
    int toLine;
}
