package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.model.LocalizationAvailableLocale;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserSetting;
import com.kurtuba.auth.utils.Utils;
import com.kurtuba.auth.utils.annotation.EmailAddress;
import com.kurtuba.auth.utils.annotation.MobileNumber;
import com.kurtuba.auth.utils.annotation.UserName;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "The full name is required.")
    @Size(min = 1, max = 100, message = "Name must be between 1-100 characters.")
    private String name;

    @Size(min = 1, max = 100, message = "Surname must be between 1-100 characters.")
    @NotBlank
    private String surname;

    @EmailAddress(notBlank = false)
    private String email;

    @MobileNumber(notBlank = false)
    private String mobile;

    @UserName(notBlank = false)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100, message = "Password length must be between 6-100 characters")
    @Pattern(regexp = Utils.PASSWORD_REGEX)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthProviderType authProvider;

    @NotBlank
    private String languageCode;

    @NotBlank
    private String countryCode;

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
                .userSetting(UserSetting.builder()
                        .canChangeUsername(false)
                        .locale(LocalizationAvailableLocale.builder()
                                .languageCode(languageCode)
                                .countryCode(countryCode)
                                .build())
                        .build())
                .activated(false)
                .locked(false)
                .failedLoginCount(0)
                .showCaptcha(false)
                .emailVerified(false)
                .mobileVerified(false)
                .createdDate(LocalDateTime.now())
                .build();
    }


}