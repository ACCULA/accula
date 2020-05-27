package org.accula.api.code;

import lombok.Value;

@Value
public class FileEntity {
    CommitMarker commit;
    String name;
    String content;

    @Override
    public String toString() {
        return commit.toString() + ":" + name;
    }
}
