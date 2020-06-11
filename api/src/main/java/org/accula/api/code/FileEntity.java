package org.accula.api.code;

import lombok.Value;
import org.accula.api.db.model.CommitSnapshot;

/**
 * @author Vadim Dyachkov
 */
@Value
public class FileEntity {
    CommitSnapshot commitSnapshot;
    String name;
    String content;

    @Override
    public String toString() {
        return commitSnapshot.toString() + ":" + name;
    }
}
