package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
public record AppSettingsDto(@OptionalField @Nullable Collection<UserDto> users,
                             @OptionalField @Nullable Collection<Long> roots,
                             Collection<Long> admins) implements InputDto {
}
