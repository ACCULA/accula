package org.accula.core.checkers;

import org.jetbrains.annotations.NotNull;

public interface CloneChecker <T, U> {
    @NotNull
    U checkClones(final @NotNull T a, final @NotNull T b, final float threshold);
}
