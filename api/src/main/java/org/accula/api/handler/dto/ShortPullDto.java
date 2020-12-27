package org.accula.api.handler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.Instant;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Vadim Dyachkov
 */
@Builder
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true, access = PRIVATE)
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
