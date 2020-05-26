package org.accula.api.detector;

import lombok.Value;
import org.accula.api.code.CommitMarker;

@Value
public class CodeSnippet {
    CommitMarker commit;
    String file;
    int fromLine;
    int toLine;
    
    public String toString() {
        return commit.toString() + ":" + file + "[" + fromLine + ":" + toLine + "]";
    }
}
