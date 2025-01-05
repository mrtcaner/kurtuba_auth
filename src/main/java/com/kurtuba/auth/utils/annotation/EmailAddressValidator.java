package com.kurtuba.auth.utils.annotation;

import com.kurtuba.auth.utils.Utils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailAddressValidator implements ConstraintValidator<EmailAddress, String> {

    boolean notEmpty;

    @Override
    public void initialize(EmailAddress constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String emailAddress, ConstraintValidatorContext context) {

        if (notEmpty && (emailAddress == null || emailAddress.isEmpty())) {
            return false;
        }

        if (!notEmpty && (emailAddress == null || emailAddress.isEmpty())) {
            return true;
        }

        //emailAddress is not null or empty
        Pattern pattern = Pattern.compile(Utils.EMAIL_REGEX);
        Matcher matcher = pattern.matcher(emailAddress);
        if(matcher.find()){
            return true;
        }

        return false;
    }
}
