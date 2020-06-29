package org.accula.suffixtree.util;

import lombok.ToString;
import lombok.Value;

@Value(staticConstructor = "of")
public class Token {
    @ToString.Exclude
    Integer type;
    String text;
    @ToString.Exclude
    Integer line;
    String filename;
    @ToString.Exclude
    String owner;
    @ToString.Exclude
    String path;

}
