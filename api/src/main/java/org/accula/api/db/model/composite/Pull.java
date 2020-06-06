package org.accula.api.db.model.composite;

import io.micrometer.core.lang.Nullable;
import lombok.Value;
import org.accula.api.db.model.CommitOld;
import org.accula.api.db.model.GithubUser;

import java.time.Instant;

@Value
public class Pull {
    @Nullable
    Long id;
    Long projectId;
    Long number;
    String title;
    Boolean open;
    Instant createdAt;
    Instant updatedAt;
    Marker head;
    Marker base;
    GithubUser author;

    @Value
    public static class Marker {
        CommitOld commit;
        String branch;
    }
}
