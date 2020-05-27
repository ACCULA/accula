package org.accula.api.code;

import lombok.Value;
import org.accula.api.db.model.Commit;

/**
 * @author Vadim Dyachkov
 */
@Value
public class FileEntity {
    Commit commit;
    String name;
    String content;

    @Override
    public String toString() {
        return commit.toString() + ":" + name;
    }
}
