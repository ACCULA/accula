package org.accula.api.psi;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Builder
@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Token<Ref> implements Comparable<Token<Ref>> {
    Ref ref;
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
