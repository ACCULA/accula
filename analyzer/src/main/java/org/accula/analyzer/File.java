package org.accula.analyzer;

import lombok.Value;

@Value
public class File<T> {
    String name;
    String path;
    String owner;
    String repo;
    T content;
}
