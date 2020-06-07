package org.accula.api.handlers.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class UserDto {
    Long id;
    String login;
    String name;
}
