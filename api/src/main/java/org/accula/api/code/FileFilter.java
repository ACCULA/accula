package org.accula.api.code;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;

/**
 * Interface to filter files by their names.
 * If {@link FileFilter#test} returns false, file won't be processed.
 *
 * @author Vadim Dyachkov
 */
@FunctionalInterface
public interface FileFilter extends Predicate<String> {
    FileFilter ALL = file -> true;
    FileFilter JAVA = file -> file.endsWith(".java");
    FileFilter SRC = file -> file.contains("src/main/java");
    FileFilter TESTS = file -> file.contains("src/test/java");
    FileFilter INFO = file -> file.endsWith("package-info.java") || file.endsWith("module-info.java");
    FileFilter SRC_JAVA = JAVA.and(SRC).and(INFO.negate());

    static FileFilter exclude(final Collection<String> excludedFiles) {
        final var set = new HashSet<>(excludedFiles);
        return f -> !set.contains(f);
    }

    @Override
    default FileFilter and(final Predicate<? super String> other) {
        return f -> test(f) && other.test(f);
    }

    @Override
    default FileFilter negate() {
        return f -> !test(f);
    }
}
