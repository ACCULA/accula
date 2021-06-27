package org.accula.api.code;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Interface to filter files by their names.
 * If {@link FileFilter#test} returns false, file won't be processed.
 *
 * @author Vadim Dyachkov
 * @author Anton Lamtev
 */
@FunctionalInterface
public interface FileFilter extends Predicate<String> {
    FileFilter ALL = file -> true;
    FileFilter NONE = file -> false;

    static FileFilter exclude(final Collection<String> excludedFiles) {
        final var set = excludedFiles instanceof Set<String> s ? s : new HashSet<>(excludedFiles);
        return f -> !set.contains(f);
    }

    static FileFilter extension(final String extension) {
        final var suffix = "." + extension;
        return file -> file.endsWith(suffix);
    }

    static FileFilter contains(final String part) {
        return file -> file.contains(part);
    }

    static FileFilter lastComponent(final String name) {
        return file -> file.endsWith(name);
    }

    @Override
    default FileFilter and(final Predicate<? super String> other) {
        return f -> test(f) && other.test(f);
    }

    @Override
    default FileFilter or(final Predicate<? super String> other) {
        return f -> test(f) || other.test(f);
    }

    @Override
    default FileFilter negate() {
        return f -> !test(f);
    }
}
