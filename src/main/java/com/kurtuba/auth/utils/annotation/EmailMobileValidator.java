package com.kurtuba.auth.utils.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailMobileValidator implements ConstraintValidator<EmailMobile, String> {

    boolean notBlank;

    @Override
    public void initialize(EmailMobile constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.notBlank = constraintAnnotation.notBlank();
    }

    @Override
    public boolean isValid(String emailMobile, ConstraintValidatorContext context) {

        if (notBlank && (emailMobile == null || emailMobile.trim().isEmpty())) {
            return false;
        }

        if (!notBlank && (emailMobile == null || emailMobile.trim().isEmpty())) {
            return true;
        }

        //wont check format, will only check if it contains @ or +
        return emailMobile.contains("@") || emailMobile.contains("+");
    }
}
