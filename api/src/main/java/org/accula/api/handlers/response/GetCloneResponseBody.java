package org.accula.api.handlers.response;

import lombok.Builder;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
//TODO: convert to dto, refactor
@Value
public class GetCloneResponseBody implements ResponseBody {
    Long id;
    FlatCodeSnippet target;
    FlatCodeSnippet source;

    @Builder
    @Value
    public static class FlatCodeSnippet {
        Long projectId;
        Integer pullNumber;
        String owner;
        String repo;
        String sha;
        String file;
        Integer fromLine;
        Integer toLine;
        String content;
    }
}
