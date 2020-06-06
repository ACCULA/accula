package org.accula.api.code;

import lombok.Value;
import org.accula.api.db.model.CommitOld;

/**
 * @author Vadim Dyachkov
 */
@Value
public class FileEntity {
    CommitOld commit;
    String name;
    String content;

    @Override
    public String toString() {
        return commit.toString() + ":" + name;
    }
}
