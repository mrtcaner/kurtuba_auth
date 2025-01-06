package com.kurtuba.auth.utils.annotation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class MobileNumberValidator implements ConstraintValidator<MobileNumber, String> {

    boolean notEmpty;

    @Override
    public void initialize(MobileNumber constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.notEmpty = constraintAnnotation.notEmpty();
    }

    @Override
    public boolean isValid(String mobileNumber, ConstraintValidatorContext context) {

        if (notEmpty && (mobileNumber == null || mobileNumber.isEmpty())) {
            return false;
        }

        if (!notEmpty && (mobileNumber == null || mobileNumber.isEmpty())) {
            return true;
        }

        //number is not null or empty
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber mNumber = phoneNumberUtil.parse(mobileNumber,
                    Phonenumber.PhoneNumber.CountryCodeSource.UNSPECIFIED.name());
            return phoneNumberUtil.isPossibleNumberForType(mNumber, PhoneNumberUtil.PhoneNumberType.MOBILE);
        } catch (NumberParseException e) {
            return false;
        }
    }
}
