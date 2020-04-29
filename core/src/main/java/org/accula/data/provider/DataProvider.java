package org.accula.data.provider;

import org.accula.data.provider.filter.GFileFilter;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;

public interface DataProvider<T, U> {
    @NotNull
    Flux<T> fetchPullRequests();
    @NotNull
    Flux<U> fetchRepoContent(@NotNull final Integer prNumber,
                             @NotNull final String repositoryName,
                             @NotNull final GFileFilter filter);
}
