package org.accula.api.code;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.accula.api.db.model.CommitSnapshot;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vadim Dyachkov
 */
@Value
public class FileEntity {
    CommitSnapshot commitSnapshot;
    @Nullable
    String name;
    @EqualsAndHashCode.Exclude
    @Nullable
    String content;

    public static FileEntity absent(final CommitSnapshot commitSnapshot) {
        return new FileEntity(commitSnapshot, null, null);
    }

    @Override
    public String toString() {
        return commitSnapshot + ":" + name;
    }
}
