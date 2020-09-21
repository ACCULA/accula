package org.accula.api.clone;

import lombok.Value;
import org.accula.api.db.model.CommitSnapshot;

/**
 * @author Vadim Dyachkov
 */
@Value
public class CodeSnippet {
    CommitSnapshot commitSnapshot;
    String file;
    int fromLine;
    int toLine;

    public int getLineCount() {
        return toLine - fromLine + 1;
    }

    @Override
    public String toString() {
        return commitSnapshot.toString() + ":" + file + "[" + fromLine + ":" + toLine + "]";
    }
}
