package org.accula.analyzer;

import lombok.Value;
import org.accula.parser.Token;

@Value
public class Clone {
    Token from;
    Token to;

    public String getFileName() {
        return from.getFilename();
    }

    public String getPath() {
        return from.getPath();
    }

    public String getOwner() {
        return from.getOwner();
    }
}
