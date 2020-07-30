package org.accula.analyzer.current;

import lombok.Value;

/**
 * @author Vadim Dyachkov
 */
@Value
public class CodeSnippet {
    String file;
    int fromLine;
    int toLine;

    public int getLineCount() {
        return toLine - fromLine + 1;
    }

    @Override
    public String toString() {
        return file + "[" + fromLine + ":" + toLine + "]";
    }
}
