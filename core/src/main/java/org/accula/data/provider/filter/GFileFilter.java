package org.accula.data.provider.filter;

import org.jetbrains.annotations.NotNull;

public interface GFileFilter {
    boolean accept(@NotNull final String fileName);
}
