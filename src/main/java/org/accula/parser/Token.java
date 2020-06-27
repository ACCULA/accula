package org.accula.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class Token implements Comparable<Token> {
    private final Integer type;
    @Setter
    private String text;
    private final Integer line;
    private final String filename;
    private final String owner;
    private final String path;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return text.equals(token.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public int compareTo(@NotNull Token o) {
        return Comparator
                .comparing(Token::getText)
                .compare(this, o);
    }

    @Override
    public String toString() {
        return "\"" + text + "\"";
    }
}
