package org.accula.analyzer;

import org.accula.parser.File;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DataProvider {
    public static List<File> getFiles(final String dir, final String fileExtension) throws IOException {
        final var files = new LinkedList<File>();
        Files.walkFileTree(Path.of(dir), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(fileExtension)) {
                    String fileContent;
                    try (var fileStream = Files.lines(file, StandardCharsets.ISO_8859_1)) {
                        fileContent = fileStream.collect(Collectors.joining("\n"));
                    }
                    files.add(new File(
                            file.getFileName().toString(),
                            file.toAbsolutePath().toString(),
                            file.getParent().getFileName().toString(),
                            fileContent
                    ));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }
}
