package com.kurtuba.auth.data.model;

import com.kurtuba.auth.data.enums.AuthProviderType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserValidationTest {

    private Validator validator;
    private User user;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        user = User.builder()
                .id(UUID.randomUUID().toString())
                .email("test@test.com")
                .emailVerified(false)
                .mobile("+905555555555")
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
                .createdDate(Instant.now())
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

        user.setName("");// empty string
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setName("   ");// string with white space only
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenMoreThanOneCharName_thenValidated() {
        user.setName("a   ");// string with white space and 1 char
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setName("a");// string with 1 char
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setName("a a");
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenAnySurName_thenValidated() {
        user.setSurname(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setSurname("");// empty string
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setSurname("   ");// string with white space only
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setSurname("asd   ");// string with chars and whitespace
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenNullOrEmptyUsername_thenValidated() {
        user.setUsername(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setUsername("");// empty string
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenBlankOrLessThanTwoCharUsername_thenInvalidated() {

        user.setUsername("     "); // whitespaces only
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setUsername("a"); // less than two char
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenCharsWithBlankUsername_thenInvalidated() {

        user.setUsername("a "); // whitespaces and a char
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setUsername("a aa"); // chars connected with whitespace
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setUsername(" aaa"); // starts with whitespace
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setUsername("aaa "); // ends with whitespace
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }



    @Test
    public void validate_whenGivenCapitalCharUsername_thenInvalidated() {
        user.setUsername("Aaa");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenSpecialCharOtherThanUnderscoreAndDotUsername_thenInvalidated() {
        user.setUsername("a*aa"); // *
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setUsername("aa?"); // ?
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        // can be extended with more specials char
    }

    @Test
    public void validate_whenGivenUnderscoreAndOrDotUsername_thenValidated() {
        user.setUsername("a.aa"); // .
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setUsername("aa_a"); // _
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());
        // can be extended with more specials char
    }

    @Test
    public void validate_whenGivenUnderscoreAndOrDotAtTheBeginningOrEndUsername_thenInvalidated() {
        user.setUsername(".aaa"); // .
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setUsername("aaa."); // _
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setUsername("aaa_"); // _
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setUsername("_aaa"); // _
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenNumberAtAnyPositionUsername_thenValidated() {
        user.setUsername("1a.aa");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setUsername("aa_a1");
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setUsername("aa1_a");
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setUsername("1aa2_a1");
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setUsername("111");
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenNullBirthdate_thenValidated() {
        user.setBirthdate(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenNullMobile_thenValidated() {
        user.setMobile(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenTurkishMobileWithPlusCountryCode_thenValidated() {
        user.setMobile("+905333333333");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenTurkishMobileWithoutPlusCountryCode_thenInvalidated() {
        user.setMobile("05033333333");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setMobile("00905033333333");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setMobile("905033333333");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenNullEmail_thenValidated() {
        user.setEmail(null);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenWellFormedEmail_thenValidated() {
        user.setEmail("user@user.com");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setEmail("1a._a_1_@ra.ca");
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());

        user.setEmail("_1a._a_1_@ra.ca");
        violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void validate_whenGivenMalformedEmail_thenValidated() {
        user.setEmail("@user.ca");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("user@.com");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("user@com.r");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail(".aa@ra.ca");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("aa.@ra.ca");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail(" aaa@ra.ca");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("a aa@ra.ca");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("aaa @ra.ca");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("aaa@ ra.ca");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("aaa@r a.ca");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("aaa@ra .ca");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("aaa@ra. ca");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("aaa@ra.c a");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("aaa@ra.ca ");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());

        user.setEmail("aaa@ra?ca ");
        violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        // can add more. https://regex101.com is good place to understand and test regexes
    }

}
