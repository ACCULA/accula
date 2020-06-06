package org.accula.api.detector;

import lombok.Value;
import org.accula.api.db.model.CommitOld;

@Value
public class CodeSnippet {
    CommitOld commit;
    String file;
    int fromLine;
    int toLine;

    public int getLineCount() {
        return toLine - fromLine + 1;
    }

    @Override
    public String toString() {
        return commit.toString() + ":" + file + "[" + fromLine + ":" + toLine + "]";
    }
}
