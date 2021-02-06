package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Vadim Dyachkov
 */
@JsonAutoDetect(fieldVisibility = ANY)
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
