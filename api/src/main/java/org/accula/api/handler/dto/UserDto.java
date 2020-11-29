package org.accula.api.handler.dto;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import static lombok.AccessLevel.PRIVATE;

/**
 * @author Anton Lamtev
 */
@Value
@RequiredArgsConstructor
@NoArgsConstructor(force = true, access = PRIVATE)
public class UserDto {
    Long id;
    String login;
    @Nullable
    String name;
    String avatar;
}
