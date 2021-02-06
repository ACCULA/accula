package org.accula.api.clone;

import lombok.Value;
import org.accula.api.db.model.Snapshot;

/**
 * @author Vadim Dyachkov
 */
@Value
public class CodeSnippet {
    Snapshot snapshot;
    String file;
    int fromLine;
    int toLine;

    public int lineCount() {
        return toLine - fromLine + 1;
    }

    @Override
    public String toString() {
        return snapshot.toString() + ":" + file + "[" + fromLine + ":" + toLine + "]";
    }
}
