package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.Collection;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
public record AppSettingsDto(@OptionalField Collection<UserDto> users,
                             @OptionalField Collection<Long> roots,
                             Collection<Long> admins) implements InputDto {
}
