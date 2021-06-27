package org.accula.api.code;

/**
 * @author Anton Lamtev
 */
@FunctionalInterface
public interface JvmFileFilter extends FileFilter {
    FileFilter JAVA = FileFilter.extension("java");
    FileFilter KOTLIN = FileFilter.extension("kt");

    FileFilter JAVA_MAIN_DIR = FileFilter.contains("src/main/java");
    FileFilter KOTLIN_MAIN_DIR = FileFilter.contains("src/main/kotlin");
    FileFilter MAIN_DIR = JAVA_MAIN_DIR.or(KOTLIN_MAIN_DIR);

    FileFilter JAVA_TEST_DIR = FileFilter.contains("src/test/java");
    FileFilter KOTLIN_TEST_DIR = FileFilter.contains("src/test/kotlin");
    FileFilter TEST_DIR = JAVA_TEST_DIR.or(KOTLIN_TEST_DIR);

    FileFilter JAVA_INFO = FileFilter.lastComponent("package-info.java").or(FileFilter.lastComponent("module-info.java"));
    FileFilter JAVA_MAIN = JAVA.and(MAIN_DIR).and(JAVA_INFO.negate());
    FileFilter KOTLIN_MAIN = KOTLIN.and(MAIN_DIR);
    FileFilter MAIN = JAVA_MAIN.or(KOTLIN_MAIN);
}
