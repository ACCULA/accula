package org.accula.api.detector;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.accula.api.db.model.CommitSnapshot;
import org.accula.api.psi.Token;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
public
class Clone {
    CloneClass parent;
    Token<CommitSnapshot> from;
    Token<CommitSnapshot> to;
    @Getter(lazy = true)
    int lineCount = getToLine() - getFromLine() + 1;
    @Getter(lazy = true)
    int fromLine = getFrom().getFromLine();
    @Getter(lazy = true)
    int toLine = getTo().getToLine();
}
