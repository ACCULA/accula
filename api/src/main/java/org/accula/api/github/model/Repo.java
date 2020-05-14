package org.accula.api.github.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class Repo {
    private String url;
    private String name;
    @Nullable
    private String description;
    private Owner owner;
}
