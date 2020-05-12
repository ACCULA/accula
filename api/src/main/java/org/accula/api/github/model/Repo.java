package org.accula.api.github.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
@Data
@NoArgsConstructor
public final class Repo {
    private String url;
    private String name;
    @Nullable
    private String description;
    private Owner owner;
}
