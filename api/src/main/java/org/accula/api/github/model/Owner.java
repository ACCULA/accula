package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Anton Lamtev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class Owner {
    private String login;
    @JsonProperty("avatar_url")
    private String avatarUrl;
}
