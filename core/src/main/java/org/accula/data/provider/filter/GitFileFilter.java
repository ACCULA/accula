package org.accula.data.provider.filter;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@Builder
public class GitFileFilter implements GFileFilter {
    @NotNull
    private List<String> includeFilter;
    @NotNull
    private List<String> excludeFilter;

    @Override
    public boolean accept(@NotNull final String file) {
        var check = includeFilter.stream().anyMatch(file::endsWith);
        return check && excludeFilter.stream().noneMatch(file::endsWith);
    }
}
