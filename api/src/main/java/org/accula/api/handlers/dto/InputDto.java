package org.accula.api.handlers.dto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Anton Lamtev
 */
public interface InputDto {
    default boolean isValid() {
        for (final var field : this.getClass().getDeclaredFields()) {
            if (field.getAnnotation(OptionalField.class) != null) {
                continue;
            }
            try {
                if (!field.canAccess(this)) {
                    field.setAccessible(true);
                }
                if (field.get(this) == null) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface OptionalField {
    }
}
