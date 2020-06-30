package org.accula.analyzer;

import lombok.Value;
import org.accula.parser.Token;

import java.util.Objects;

@Value
public class Clone {
    Token from;
    Token to;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clone clone = (Clone) o;
        return from.getText().equals(clone.from.getText()) &&
                to.getText().equals(clone.to.getText());
    }

    @Override
    public int hashCode() {
        return Objects.hash(from.getText(), to.getText());
    }

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
