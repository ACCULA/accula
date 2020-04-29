package org.accula.data.model;

import org.jetbrains.annotations.NotNull;

public record GFile<T> (@NotNull String userName,
                        @NotNull Integer prNumber,
                        @NotNull String fileName,
                        @NotNull String link,
                        @NotNull T content) {}
