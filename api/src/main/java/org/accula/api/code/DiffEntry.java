package org.accula.api.code;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class DiffEntry<Ref> {
    FileEntity<Ref> base;
    FileEntity<Ref> head;
    int similarityIndex;

    public static <Ref> DiffEntry<Ref> of(final FileEntity<Ref> base, final FileEntity<Ref> head) {
        return new DiffEntry<>(base, head, 0);
    }
}
