package org.accula.api.handler.dto.validation;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BeanPropertyBindingResult;

import java.io.Serial;
import java.util.stream.Collectors;

/**
 * @author Anton Lamtev
 */
public final class Errors extends BeanPropertyBindingResult {
    @Serial
    private static final long serialVersionUID = -5742876874765556966L;

    public Errors(final Object target, final String objectName) {
        super(target, objectName);
    }

    public String joinedDescription() {
        return getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
    }
}
