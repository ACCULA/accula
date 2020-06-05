package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * @author Anton Lamtev
 */
@Data
@NoArgsConstructor
public final class GithubApiUserPermission {
    private Permission permission;

    public enum Permission {
        READ,
        WRITE,
        ADMIN,
        ;

        @JsonValue
        public String value() {
            return name().toLowerCase(Locale.US);
        }
    }
}
