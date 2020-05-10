package org.accula.analyzer.checkers.utils;

import lombok.Value;

@Value
public class Clone {
    String owner;
    String fileName;
    String path;
    Integer fromLine;
    Integer toLine;
}
