package org.accula.api.psi;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.accula.api.db.model.CommitSnapshot;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Token implements Comparable<Token> {
    CommitSnapshot commitSnapshot;
    @EqualsAndHashCode.Include
    String string;
    String methodName;
    String filename;
    int fromLine;
    int toLine;

    @Override
    public int compareTo(final Token otherToken) {
        return string.compareTo(otherToken.string);
    }
}
