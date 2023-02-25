package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.jetbrains.annotations.Nullable;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
public record UserDto(Long id, String login, @Nullable String name, String avatar, @Nullable Role role) {
    public enum Role {
        USER,
        ADMIN,
        ROOT,
    }
}
