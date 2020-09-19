package org.accula.api.detector;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.accula.api.db.model.CommitSnapshot;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
public class Clone {
    @EqualsAndHashCode.Exclude
    CloneClass parent;
    Token<CommitSnapshot> start;
    Token<CommitSnapshot> end;
    @Getter(lazy = true)
    int lineCount = getToLine() - getFromLine() + 1;
    @Getter(lazy = true)
    int fromLine = getStart().getFromLine();
    @Getter(lazy = true)
    int toLine = getEnd().getToLine();

    public CommitSnapshot commitSnapshot() {
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
