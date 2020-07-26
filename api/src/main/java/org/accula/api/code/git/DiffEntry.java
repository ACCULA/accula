package org.accula.api.code.git;

import lombok.Value;

import java.util.stream.Stream;

/**
 * @author Anton Lamtev
 */
interface DiffEntry {
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

    Stream<String> objectIds();
}

@Value(staticConstructor = "of")
class Modification implements DiffEntry {
    public File base;
    public File head;

    @Override
    public Stream<String> objectIds() {
        return Stream.of(base.objectId, head.objectId);
    }
}

@Value(staticConstructor = "of")
class Addition implements DiffEntry {
    public File head;

    @Override
    public Stream<String> objectIds() {
        return Stream.of(head.objectId);
    }
}

@Value(staticConstructor = "of")
class Deletion implements DiffEntry {
    public File base;

    @Override
    public Stream<String> objectIds() {
        return Stream.of(base.objectId);
    }
}

@Value(staticConstructor = "of")
class Renaming implements DiffEntry {
    public File base;
    public File head;
    public int similarityIndex;

    @Override
    public Stream<String> objectIds() {
        return Stream.of(base.objectId, head.objectId);
    }
}
