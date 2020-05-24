package org.accula.api.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class Pull {
    @Id
    @Nullable
    private Long id;
    private Long projectId;
    private Integer number;
}
