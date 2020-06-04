package org.accula.api.db.model.composite;

import io.micrometer.core.lang.Nullable;
import lombok.Value;

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

}
