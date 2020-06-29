package org.accula.suffixtree.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ReferenceClone {

    private final String name;
    private final String owner;
    private final int fromLine;
    private final int toLine;

    public int getCloneLengthInLines() {
        return toLine - fromLine + 1;
    }
}
