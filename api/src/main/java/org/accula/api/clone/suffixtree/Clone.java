package org.accula.api.clone.suffixtree;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.accula.api.token.Token;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
public class Clone<Ref> {
    @EqualsAndHashCode.Exclude
    CloneClass<Ref> parent;
    Token<Ref> start;
    Token<Ref> end;
    @Getter(lazy = true)
    int lineCount = toLine() - fromLine() + 1;
    @Getter(lazy = true)
    int fromLine = start().fromLine();
    @Getter(lazy = true)
    int toLine = end().toLine();

    public Ref ref() {
        if (!start.ref().equals(end.ref())) {
            throw new IllegalStateException("start.ref MUST be equal to end.ref");
        }
        return start.ref();
    }

    public String filename() {
        if (!start.filename().equals(end.filename())) {
            throw new IllegalStateException("start.filename MUST be equal to end.filename");
        }
        return start.filename();
    }
}
