package com.ment.chat.client.model.annotation;

import com.ment.chat.client.model.enums.LlmProvider;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EnumSet;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

@Constraint(validatedBy = ValidProviderSize.ProviderSizeValidator.class)
@Target({ElementType.FIELD, ANNOTATION_TYPE, PARAMETER, TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProviderSize {
    String message() default "Number of providers must be greater or equal to 2 or less or equal than the number of valid providers";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class ProviderSizeValidator implements ConstraintValidator<ValidProviderSize, EnumSet<LlmProvider>> {
        @Override
        public boolean isValid(EnumSet<LlmProvider> values, ConstraintValidatorContext context) {
            if (values == null) return true;
            return values.size() >= 2 && values.size() <= LlmProvider.values().length;
        }
    }

}
