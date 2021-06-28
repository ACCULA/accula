package org.accula.api.token;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.accula.api.code.lines.LineRange;

/**
 * @author Anton Lamtev
 */
@Value(staticConstructor = "of")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Token<Ref> implements Comparable<Token<Ref>> {
    @EqualsAndHashCode.Include
    String string;
    String filename;
    String methodName;
    LineRange lines;
    Ref ref;

    @Override
    public int compareTo(final Token otherToken) {
        return string.compareTo(otherToken.string);
    }
}
