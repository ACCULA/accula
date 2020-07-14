package org.accula.api.handlers.dto;

import lombok.Builder;
import lombok.Value;

/**
 * @author Vadim Dyachkov
 */
@Builder
@Value
public class ShortPullDto {
    Long projectId;
    Integer number;
    String url;
    String title;
    Boolean open;
    GithubUserDto author;
}
