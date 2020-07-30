package org.accula.parser;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

@Value
public class Token implements Comparable<Token> {
    int type;
    String text;
    int line;
    String filename;
    String owner;
    String path;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return type == token.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public int compareTo(@NotNull Token o) {
        return Comparator
                .comparingInt(Token::getType)
                .compare(this, o);
    }

    @Override
    public String toString() {
        return "\"" + text + "\"";
    }
}
