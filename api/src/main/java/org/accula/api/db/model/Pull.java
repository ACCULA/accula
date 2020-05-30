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
    @Id
    @Nullable
    private Long id;
    private Long projectId;
    private Integer number;
    private Long headLastCommitId;
    private String baseLastCommitSha;
    private Instant updatedAt;
}
