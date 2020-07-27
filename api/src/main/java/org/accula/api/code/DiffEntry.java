package org.accula.api.code;

import lombok.Value;

@Value
public class DiffEntry {
    FileEntity base;
    FileEntity head;
    int similarityIndex;

    public static DiffEntry of(final FileEntity base, final FileEntity head) {
        return new DiffEntry(base, head, 0);
    }
}
