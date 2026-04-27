package com.turnos.saas.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.regex.Pattern;

@Documented
@Constraint(validatedBy = NoHtml.NoHtmlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHtml {

    String message() default "El campo contiene contenido HTML o scripts no permitidos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

        private static final Pattern HTML_TAG        = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE);
        private static final Pattern HTML_ENTITY     = Pattern.compile("&[a-zA-Z#][a-zA-Z0-9]{1,6};");
        private static final Pattern JAVASCRIPT_URL  = Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);
        private static final Pattern EVENT_HANDLER   = Pattern.compile("on[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);
        private static final Pattern DATA_URL        = Pattern.compile("data\\s*:[^,]*,", Pattern.CASE_INSENSITIVE);

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true;
            }
            return !HTML_TAG.matcher(value).find()
                    && !HTML_ENTITY.matcher(value).find()
                    && !JAVASCRIPT_URL.matcher(value).find()
                    && !EVENT_HANDLER.matcher(value).find()
                    && !DATA_URL.matcher(value).find();
        }
    }
}
