package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import java.util.Locale;

/**
 * @author Anton Lamtev
 */
@Value
public class GithubApiUserPermission {
    Permission permission;

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
