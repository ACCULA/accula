package org.accula.api.handler.dto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Consumer;

/**
 * @author Anton Lamtev
 */
public interface InputDto {
    default void enumerateMissingRequiredFields(final Consumer<String> fieldName) {
        for (final var field : this.getClass().getDeclaredFields()) {
            if (field.getAnnotation(OptionalField.class) != null) {
                continue;
            }
            try {
                if (!field.canAccess(this)) {
                    field.setAccessible(true);
                }
                if (field.get(this) == null) {
                    fieldName.accept(field.getName());
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface OptionalField {
    }
}
