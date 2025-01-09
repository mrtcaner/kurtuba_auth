package com.kurtuba.auth.data.model;

import com.kurtuba.auth.data.enums.AuthProviderType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class UserAndUserSettingsValidationTest {

    private Validator validator;
    private User user;

    @Before
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        user = User.builder()
                .id(UUID.randomUUID().toString())
                .email("test@test.com")
                .emailVerified(false)
                .mobile("+905366568898")
                .mobileVerified(false)
                .activated(false)
                .name("user")
                .surname("kaufmann")
                .authProvider(AuthProviderType.KURTUBA)
                .locked(false)
                .failedLoginCount(0)
                .locked(false)
                .showCaptcha(false)
                .birthdate(null)
                .password("12345")
                .username("username")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    public void validate_whenGivenValid_thenValidated() {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenNullOrEmptyOrLessThanTwoCharName_thenInvalidated() {
        user.setName(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        user.setName("");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        user.setName("a");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenMoreThanOneCharName_thenValidated() {
        user.setName("aa");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

}
