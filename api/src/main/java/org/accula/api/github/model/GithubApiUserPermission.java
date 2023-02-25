package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * @author Anton Lamtev
 */
public record GithubApiUserPermission(Permission permission) {
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
