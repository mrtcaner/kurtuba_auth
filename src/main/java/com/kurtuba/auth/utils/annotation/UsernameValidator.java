package com.kurtuba.auth.utils.annotation;

import com.kurtuba.auth.utils.Utils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<UserName, String> {

    boolean notEmpty;

    @Override
    public void initialize(UserName constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.notEmpty = constraintAnnotation.notEmpty();
    }

    @Override
    public boolean isValid(String userName, ConstraintValidatorContext context) {

        if (notEmpty && (userName == null || userName.isEmpty())) {
            return false;
        }

        if (!notEmpty && (userName == null || userName.isEmpty())) {
            return true;
        }

        //userName is not null or empty
        Pattern pattern = Pattern.compile(Utils.USERNAME_REGEX);
        Matcher matcher = pattern.matcher(userName);
        if(matcher.find()){
            return true;
        }

        return false;
    }
}
