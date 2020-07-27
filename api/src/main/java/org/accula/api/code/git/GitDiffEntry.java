package org.accula.api.code.git;

import lombok.Value;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
public interface GitDiffEntry {
    static GitDiffEntry modification(final String baseObjectId, final String headObjectId, final String filename) {
        return Modification.of(File.of(baseObjectId, filename), File.of(headObjectId, filename));
    }

    static GitDiffEntry addition(final String headObjectId, final String filename) {
        return Addition.of(File.of(headObjectId, filename));
    }

    static GitDiffEntry deletion(final String baseObjectId, final String filename) {
        return Deletion.of(File.of(baseObjectId, filename));
    }

    static GitDiffEntry renaming(final String baseObjectId,
                                 final String baseFilename,
                                 final String headObjectId,
                                 final String headFilename,
                                 final int similarityIndex) {
        return Renaming.of(File.of(baseObjectId, baseFilename), File.of(headObjectId, headFilename), similarityIndex);
    }

    Stream<String> objectIds();

    <T extends Predicate<String>> boolean passes(T filter);

    @Value(staticConstructor = "of")
    class Modification implements GitDiffEntry {
        public File base;
        public File head;

        @Override
        public Stream<String> objectIds() {
            return Stream.of(base.objectId, head.objectId);
        }

        @Override
        public <T extends Predicate<String>> boolean passes(final T filter) {
            return filter.test(base.name);
        }
    }

    @Value(staticConstructor = "of")
    class Addition implements GitDiffEntry {
        public File head;

        @Override
        public Stream<String> objectIds() {
            return Stream.of(head.objectId);
        }

        @Override
        public <T extends Predicate<String>> boolean passes(final T filter) {
            return filter.test(head.name);
        }
    }

    @Value(staticConstructor = "of")
    class Deletion implements GitDiffEntry {
        public File base;

        @Override
        public Stream<String> objectIds() {
            return Stream.of(base.objectId);
        }

        @Override
        public <T extends Predicate<String>> boolean passes(final T filter) {
            return filter.test(base.name);
        }
    }

    @Value(staticConstructor = "of")
    class Renaming implements GitDiffEntry {
        public File base;
        public File head;
        public int similarityIndex;

        @Override
        public Stream<String> objectIds() {
            return Stream.of(base.objectId, head.objectId);
        }

        @Override
        public <T extends Predicate<String>> boolean passes(final T filter) {
            return filter.test(base.name) || filter.test(head.name);
        }
    }
}
