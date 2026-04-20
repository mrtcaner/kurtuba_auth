package com.kurtuba.auth.utils.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailMobileValidator.class)
public @interface EmailMobile {
    boolean notBlank() default true;
    String message() default "Invalid email/mobile format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}