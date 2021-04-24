package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Builder
@Value
public class PlagiaristDto {
    GithubUserDto user;
    Integer cloneCount;
}
