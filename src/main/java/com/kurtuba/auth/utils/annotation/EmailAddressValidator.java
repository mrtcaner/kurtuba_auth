package com.kurtuba.auth.utils.annotation;

import com.kurtuba.auth.utils.Utils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailAddressValidator implements ConstraintValidator<EmailAddress, String> {

    boolean notBlank;

    @Override
    public void initialize(EmailAddress constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.notBlank = constraintAnnotation.notBlank();
    }

    @Override
    public boolean isValid(String emailAddress, ConstraintValidatorContext context) {

        if (notBlank && (emailAddress == null || emailAddress.trim().isEmpty())) {
            return false;
        }

        if (!notBlank && (emailAddress == null || emailAddress.trim().isEmpty())) {
            return true;
        }

        //emailAddress is not null or empty
        Pattern pattern = Pattern.compile(Utils.EMAIL_REGEX);
        Matcher matcher = pattern.matcher(emailAddress);
        return matcher.find();
    }
}
