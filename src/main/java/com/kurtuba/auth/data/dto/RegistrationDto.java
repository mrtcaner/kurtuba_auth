package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.utils.Utils;
import com.kurtuba.auth.utils.annotation.EmailAddress;
import com.kurtuba.auth.utils.annotation.MobileNumber;
import com.kurtuba.auth.utils.annotation.UserName;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDto {
    @NotEmpty(message = "The full name is required.")
    @Size(min = 2, max = 100, message = "Name must be between 2-100 characters.")
    private String name;

    @Size(min = 2, max = 100, message = "Surname must be between 2-100 characters.")
    @NotEmpty
    private String surname;

    @EmailAddress(notEmpty = false)
    private String email;

    @MobileNumber(notEmpty = false)
    private String mobile;

    @UserName(notEmpty = false)
    private String username;

    @NotEmpty
    @Size(min = 8, max = 100, message = "Password length must be between 8-100 characters")
    @Pattern(regexp = Utils.PASSWORD_REGEX)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthProviderType authProvider;

    @NotEmpty
    private String language;

    @NotEmpty
    private String country;

    @NotNull
    @Enumerated
    private ContactType preferredVerificationContact;

    boolean verificationByCode;

    public User toUser() {
        return User.builder()
                .name(name)
                .surname(surname)
                .username(username)
                .email(email)
                .password(password)
                .authProvider(authProvider)
                .mobile(mobile)
                .canChangeUsername(false)
                .activated(false)
                .locked(false)
                .failedLoginCount(0)
                .showCaptcha(false)
                .emailVerified(false)
                .mobileVerified(false)
                .language(language)
                .country(country)
                .createdDate(LocalDateTime.now())
                .build();
    }


}