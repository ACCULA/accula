package org.accula.api.code.git;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
public interface DiffEntry {
    static DiffEntry modification(final String baseObjectId, final String headObjectId, final String filename) {
        return Modification.of(File.of(baseObjectId, filename), File.of(headObjectId, filename));
    }

    static DiffEntry addition(final String headObjectId, final String filename) {
        return Addition.of(File.of(headObjectId, filename));
    }

    static DiffEntry deletion(final String baseObjectId, final String filename) {
        return Deletion.of(File.of(baseObjectId, filename));
    }

    static DiffEntry renaming(final String baseObjectId,
                              final String baseFilename,
                              final String headObjectId,
                              final String headFilename,
                              final int similarityIndex) {
        return Renaming.of(File.of(baseObjectId, baseFilename), File.of(headObjectId, headFilename), similarityIndex);
    }

    Stream<Identifiable> objectIds();

    <F extends Predicate<String>> boolean passes(F filter);

    @Value
    @RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
    class Modification implements DiffEntry {
        File base;
        File head;

        @Override
        public Stream<Identifiable> objectIds() {
            return Stream.of(base, head);
        }

        @Override
        public <F extends Predicate<String>> boolean passes(final F filter) {
            return filter.test(base.getName());
        }
    }

    @Value
    @RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
    class Addition implements DiffEntry {
        File head;

        @Override
        public Stream<Identifiable> objectIds() {
            return Stream.of(head);
        }

        @Override
        public <F extends Predicate<String>> boolean passes(final F filter) {
            return filter.test(head.getName());
        }
    }

    @Value
    @RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
    class Deletion implements DiffEntry {
        File base;

        @Override
        public Stream<Identifiable> objectIds() {
            return Stream.of(base);
        }

        @Override
        public <F extends Predicate<String>> boolean passes(final F filter) {
            return filter.test(base.getName());
        }
    }

    @Value
    @RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
    class Renaming implements DiffEntry {
        File base;
        File head;
        int similarityIndex;

        @Override
        public Stream<Identifiable> objectIds() {
            return Stream.of(base, head);
        }

        @Override
        public <F extends Predicate<String>> boolean passes(final F filter) {
            return filter.test(base.getName()) || filter.test(head.getName());
        }
    }
}
