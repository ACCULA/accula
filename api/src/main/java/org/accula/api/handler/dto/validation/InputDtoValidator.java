package org.accula.api.handler.dto.validation;

import org.accula.api.handler.dto.InputDto;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * @author Anton Lamtev
 */
public final class InputDtoValidator implements Validator {
    @Override
    public boolean supports(final Class<?> clazz) {
        return InputDto.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final var input = (InputDto) target;
        input.enumerateMissingRequiredFields(field ->
                ValidationUtils.rejectIfEmpty(errors, field, "%s.empty".formatted(field), "%s is empty".formatted(field)));
    }
}
