package org.accula.api.handler.dto;

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
