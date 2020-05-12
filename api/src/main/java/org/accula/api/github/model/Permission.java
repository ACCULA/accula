package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Anton Lamtev
 */
@Data
@NoArgsConstructor
public final class Permission {
    private PermissionEnum permission;

    public enum PermissionEnum {
        READ,
        WRITE,
        ADMIN,
        ;

        @JsonValue
        public String value() {
            return name().toLowerCase();
        }
    }
}
