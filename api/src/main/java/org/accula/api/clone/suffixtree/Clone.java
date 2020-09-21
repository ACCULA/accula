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
    int lineCount = getToLine() - getFromLine() + 1;
    @Getter(lazy = true)
    int fromLine = getStart().getFromLine();
    @Getter(lazy = true)
    int toLine = getEnd().getToLine();

    public Ref ref() {
        if (!start.getRef().equals(end.getRef())) {
            throw new IllegalStateException("start.ref MUST be equal to end.ref");
        }
        return start.getRef();
    }

    public String filename() {
        if (!start.getFilename().equals(end.getFilename())) {
            throw new IllegalStateException("start.filename MUST be equal to end.filename");
        }
        return start.getFilename();
    }
}
