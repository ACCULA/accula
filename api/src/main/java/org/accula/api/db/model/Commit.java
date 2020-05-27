package org.accula.api.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;

/**
 * @author Vadim Dyachkov
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commit {
    @Id
    @Nullable
    private Long id;
    private String owner;
    private String repo;
    private String sha;

    @Override
    public String toString() {
        return owner + "/" + repo + "/" + sha;
    }
}
