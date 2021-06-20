package org.accula.api.handler.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

/**
 * @author Anton Lamtev
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Value
public class UserDto {
    Long id;
    String login;
    @Nullable
    String name;
    String avatar;
    Role role;

    public enum Role {
        USER,
        ADMIN,
        ROOT,
    }
}
