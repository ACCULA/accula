package org.accula.api.code;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.accula.api.code.lines.LineSet;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@Value
public class FileEntity<Ref> {
    Ref ref;
    @Nullable
    String name;
    @EqualsAndHashCode.Exclude
    @Nullable
    String content;
    LineSet lines;

    public static <Ref> FileEntity<Ref> absent(final Ref ref) {
        return new FileEntity<>(ref, null, null, null);
    }

    @Override
    public String toString() {
        return ref + ":" + name;
    }
}
