package ru.mail.polis.gt;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.NoSuchElementException;

// Example from https://github.com/polis-mail-ru/2017-highload-kv/pull/79/files#diff-c77220205c66f1669ae79993fa8b65d7
public class GregFileDAO implements GregDAO {
    private final File dir;

    public GregFileDAO(File dir) {
        this.dir = dir;
    }

    @NotNull
    private File getFile(@NotNull final String key) {
        return new File(dir, key);
    }

    @NotNull
    @Override
    public byte[] get(@NotNull final String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        final File file = getFile(key);
        final byte[] value = new byte[(int) file.length()];
        try(InputStream is = new FileInputStream(file)) {
            if(is.read(value) != value.length) {
                //throw new IOException("Can't read file in one go");
            }
        } catch (FileNotFoundException e) {
            throw new NoSuchElementException();
        }
        return value;
    }

    @NotNull
    @Override
    public void upsert(@NotNull final String key, @NotNull final byte[] value) throws IllegalArgumentException, IOException {
        try(OutputStream os = new FileOutputStream(getFile(key))) {
            os.write(value);
        }
    }

    @NotNull
    @Override
    public void delete(@NotNull final String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        getFile(key).delete();
    }
}
