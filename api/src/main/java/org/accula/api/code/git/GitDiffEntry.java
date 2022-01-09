package org.accula.api.code.git;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
public sealed interface GitDiffEntry
    permits GitDiffEntry.Modification,
            GitDiffEntry.Addition,
            GitDiffEntry.Deletion,
            GitDiffEntry.Renaming {
    static GitDiffEntry modification(final String baseObjectId, final String headObjectId, final String filename) {
        return Modification.of(GitFile.of(baseObjectId, filename), GitFile.of(headObjectId, filename));
    }

    static GitDiffEntry addition(final String headObjectId, final String filename) {
        return Addition.of(GitFile.of(headObjectId, filename));
    }

    static GitDiffEntry deletion(final String baseObjectId, final String filename) {
        return Deletion.of(GitFile.of(baseObjectId, filename));
    }

    static GitDiffEntry renaming(final String baseObjectId,
                                 final String baseFilename,
                                 final String headObjectId,
                                 final String headFilename,
                                 final int similarityIndex) {
        return Renaming.of(GitFile.of(baseObjectId, baseFilename), GitFile.of(headObjectId, headFilename), similarityIndex);
    }

    Stream<Identifiable> objectIds();

    <F extends Predicate<String>> boolean passes(F filter);

    @Value
    @RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
    class Modification implements GitDiffEntry {
        GitFile base;
        GitFile head;

        @Override
        public Stream<Identifiable> objectIds() {
            return Stream.of(base, head);
        }

        @Override
        public <F extends Predicate<String>> boolean passes(final F filter) {
            return filter.test(base.name());
        }
    }

    @Value
    @RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
    class Addition implements GitDiffEntry {
        GitFile head;

        @Override
        public Stream<Identifiable> objectIds() {
            return Stream.of(head);
        }

        @Override
        public <F extends Predicate<String>> boolean passes(final F filter) {
            return filter.test(head.name());
        }
    }

    @Value
    @RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
    class Deletion implements GitDiffEntry {
        GitFile base;

        @Override
        public Stream<Identifiable> objectIds() {
            return Stream.of(base);
        }

        @Override
        public <F extends Predicate<String>> boolean passes(final F filter) {
            return filter.test(base.name());
        }
    }

    @Value
    @RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
    class Renaming implements GitDiffEntry {
        GitFile base;
        GitFile head;
        int similarityIndex;

        @Override
        public Stream<Identifiable> objectIds() {
            return Stream.of(base, head);
        }

        @Override
        public <F extends Predicate<String>> boolean passes(final F filter) {
            return filter.test(base.name()) || filter.test(head.name());
        }
    }
}
