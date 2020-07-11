package org.accula.api.handler.dto;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

/**
 * @author Anton Lamtev
 */
@Value
public class UserDto {
    Long id;
    String login;
    @Nullable
    String name;
}
