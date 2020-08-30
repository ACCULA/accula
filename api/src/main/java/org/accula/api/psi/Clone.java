package org.accula.api.psi;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
public
class Clone {
    CloneClass parent;
    Token from;
    Token to;
    @Getter(lazy = true)
    int lineCount = getToLine() - getFromLine() + 1;
    @Getter(lazy = true)
    int fromLine = getFrom().getFromLine();
    @Getter(lazy = true)
    int toLine = getTo().getToLine();
}
