package com.kurtuba.auth.utils.annotation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class MobileNumberValidator implements ConstraintValidator<MobileNumber, String> {

    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    boolean notBlank;
    boolean plusSignRequired;

    @Override
    public void initialize(MobileNumber constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        this.notBlank = constraintAnnotation.notBlank();
        this.plusSignRequired = constraintAnnotation.plusSignRequired();
    }

    @Override
    public boolean isValid(String mobileNumber, ConstraintValidatorContext context) {

        if (notBlank && (mobileNumber == null || mobileNumber.trim().isEmpty())) {
            return false;
        }

        if (!notBlank && (mobileNumber == null || mobileNumber.trim().isEmpty())) {
            return true;
        }

        //number is not null or empty
        try {
            Phonenumber.PhoneNumber mNumber = phoneNumberUtil.parse(mobileNumber,
                    plusSignRequired ?
                    Phonenumber.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN.name():
                            Phonenumber.PhoneNumber.CountryCodeSource.UNSPECIFIED.name()
                    );
            return phoneNumberUtil.isPossibleNumberForType(mNumber, PhoneNumberUtil.PhoneNumberType.MOBILE);
        } catch (NumberParseException e) {
            return false;
        }
    }
}
