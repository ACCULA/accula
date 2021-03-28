package org.accula.api.clone.suffixtree;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.accula.api.code.lines.LineRange;
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
    LineRange lines = LineRange.of(start.lines().from(), end.lines().to());

    public Ref ref() {
        if (!start.ref().equals(end.ref())) {
            throw new IllegalStateException("start.ref = %s MUST be equal to end.ref = %s".formatted(start.ref(), end.ref()));
        }
        return start.ref();
    }

    public String method() {
        if (!start.methodName().equals(end.methodName())) {
            throw new IllegalStateException("start.methodName = %s MUST be equal to end.methodName = %s"
                    .formatted(start.methodName(), end.methodName()));
        }
        return start.methodName();
    }

    public String filename() {
        if (!start.filename().equals(end.filename())) {
            throw new IllegalStateException("start.filename = %s MUST be equal to end.filename = %s"
                    .formatted(start.filename(), end.filename()));
        }
        return start.filename();
    }
}
