package org.accula.api.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class Pull {
    public Pull(@Nullable Long id, Long projectId, Integer number) {
        this.id = id;
        this.projectId = projectId;
        this.number = number;
    }

    @Id
    @Nullable
    private Long id;
    private Long projectId;
    private Integer number;
    @Nullable
    private Long lastCommitId;
    private Instant updatedAt;
}
