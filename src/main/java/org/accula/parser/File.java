package org.accula.parser;

import lombok.Value;

@Value
public class File {
    String name;
    String path;
    String owner;
    String content;
}
