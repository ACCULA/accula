package org.accula.api.handler.dto.validation;

import org.accula.api.handler.dto.InputDto;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * @author Anton Lamtev
 */
public abstract class InputDtoValidator implements Validator {
    public static Validator forClass(final Class<?> validatorClass) {
        return new InputDtoValidator() {
            @Override
            public boolean supports(final Class<?> clazz) {
                return validatorClass.equals(clazz);
            }
        };
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final var input = (InputDto) target;
        input.enumerateMissingRequiredFields(field ->
                ValidationUtils.rejectIfEmpty(errors, field, "%s.empty".formatted(field), "%s is empty".formatted(field)));
    }
}
