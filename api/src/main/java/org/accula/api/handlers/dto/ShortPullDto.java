package org.accula.api.handlers.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

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
    Instant createdAt;
    Instant updatedAt;
    Boolean open;
    GithubUserDto author;
}
