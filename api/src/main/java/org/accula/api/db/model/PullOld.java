package org.accula.api.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("pull")
public final class PullOld {
    @Id
    @Nullable
    private Long id;
    private Long projectId;
    private Integer number;
    private Long headLastCommitId;
    private String baseLastCommitSha;
    private Instant updatedAt;
}
