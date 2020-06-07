package org.accula.api.handlers.dto;

import lombok.Value;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubUserDto {
    String login;
    String avatar;
    String url;
}
