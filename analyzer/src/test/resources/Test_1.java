package ru.mail.polis.mikhail.DAO;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

// Example from https://github.com/polis-mail-ru/2017-highload-kv/pull/81/files#diff-ba9d84095cba4a413dbe5c870bdb4348
public class MyFileDAO implements MyDAO {

    @NotNull
    private final File directory;
    @NotNull
    private final Map<String, byte[]> cache;

    public MyFileDAO(@NotNull final File directory) {
        this.directory = directory;
        this.cache = new ConcurrentHashMap<>();
    }

    @NotNull
    private Path getPath(@NotNull final String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key is empty");
        }
        return Paths.get(directory.getPath(), key);
    }

    @NotNull
    @Override
    public byte[] get(@NotNull final String key)
            throws NoSuchElementException, IllegalArgumentException, IOException {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Path path = getPath(key);
        if (!Files.exists(path)) {
            throw new NoSuchElementException("File doesn't exist");
        }
        final byte[] value = Files.readAllBytes(getPath(key));
        cache.put(key, value);
        return value;
    }

    @Override
    public void upsert(@NotNull final String key,
                       @NotNull final byte[] value)
            throws IllegalArgumentException, NoSuchElementException, IOException {
        cache.remove(key);
        Files.write(getPath(key), value);
    }

    @Override
    public void delete(@NotNull final String key)
            throws IllegalArgumentException, IOException {
        cache.remove(key);
        Files.deleteIfExists(getPath(key));
    }
}
