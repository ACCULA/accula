package org.accula.api.detector.parser;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

//Temporally unused class, will be used when generification of input data in CloneIndexer is being introduced
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Token implements Comparable<Token> {
    @EqualsAndHashCode.Include
    private Integer type;
    private final String text;
    private final Integer line;
    private final String filename;
    private final Long ownerId;
    private final Long repoId;

    @Override
    public int compareTo(@NotNull Token o) {
        return type - o.type;
    }

    @Override
    public String toString() {
        return "\"" + text + "\"";
    }
}
