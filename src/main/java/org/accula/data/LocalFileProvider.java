package org.accula.data;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.accula.parser.FileEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class LocalFileProvider implements DataProvider {
    private final String dir;
    private final String fileExtension;

    @SneakyThrows
    public Flux<FileEntity> getFiles() {
        final List<FileEntity> files = new ArrayList<>();
        return Mono
                .fromCallable(() -> Files.walkFileTree(Path.of(dir), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().endsWith(fileExtension)) {
                            String fileContent;
                            try (var fileStream = Files.lines(file)) {
                                fileContent = fileStream.collect(Collectors.joining("\n"));
                            }
                            files.add(new FileEntity(
                                    file.getFileName().toString(),
                                    file.toAbsolutePath().toString(),
                                    file.getParent().getFileName().toString(),
                                    fileContent
                            ));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }))
                .doOnError(e -> log.error("Error receiving files in directory {} : {}", dir, e.getMessage()))
                .thenMany(Flux.fromIterable(files));
    }
}
