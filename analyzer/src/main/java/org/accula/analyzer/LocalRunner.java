package org.accula.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.accula.analyzer.checkers.utils.ClonePair;
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
public class LocalRunner {
    public static void main(String[] args) {
        var dir = args[0];
        var fileExtension = args[1];
        var threshold = Float.parseFloat(args[2]);
        var minCloneLength = Integer.parseInt(args[3]);
        var data = getFiles(dir, fileExtension).take(82);
        var cloneDetector = new CloneDetector();
        cloneDetector
                .analyze(data, threshold, minCloneLength)
                .subscribe(LocalRunner::printClonePair);
    }

    private static Flux<File<String>> getFiles(final String dir, final String fileExtension) {
        final List<File<String>> files = new ArrayList<>();
        return Mono
                .fromCallable(() -> Files.walkFileTree(Path.of(dir), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.getFileName().toString().endsWith(fileExtension)) {
                            files.add(new File<>(
                                    file.getFileName().toString(),
                                    file.toAbsolutePath().toString(),
                                    file.getParent().getFileName().toString(),
                                    file.getParent().getFileName().toString(),
                                    Files.lines(file).collect(Collectors.joining("\n"))
                            ));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }))
                .doOnError(e -> log.error("Error receiving files in directory {} : {}", dir, e.getMessage()))
                .thenMany(Flux.fromIterable(files));
    }

    private static void printClonePair(final ClonePair clonePair) {
        var first = clonePair.getFirst();
        var second = clonePair.getSecond();
        var str = first.getOwner() + "," + first.getFileName() + "," +
                first.getFromLine() + "," + first.getToLine() + "," +
                second.getOwner() + "," + second.getFileName() + "," +
                second.getFromLine() + "," + second.getToLine();
        System.out.println(str);
    }
}
