package org.accula.api.handlers.response;

import lombok.Builder;
import lombok.Value;

/**
 * @author Vadim Dyachkov
 */
@Value
@Builder
public class GetDiffResponseBody implements ResponseBody {
    String baseFilename;
    String headFilename;
    String baseContent;
    String headContent;
}
