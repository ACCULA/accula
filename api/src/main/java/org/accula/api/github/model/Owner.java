package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Anton Lamtev
 */
@Data
@NoArgsConstructor
public final class Owner {
    private String login;
    @JsonProperty("avatar_url")
    private String avatarUrl;
}
