package org.accula.suffixtree.util;

import lombok.Value;

import java.util.List;

@Value
public class TokenizedFile {

    String name;
    String owner;
    List<TokenizedMethod> methods;
}
