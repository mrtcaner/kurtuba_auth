package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.model.User;
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
public class UserRegistrationDto {
    @NotEmpty(message = "The full name is required.")
    @Size(min = 2, max = 100, message = "Name must be between 2-100 characters.")
    private String name;

    @Size(min = 2, max = 100, message = "Surname must be between 2-100 characters.")
    @NotEmpty
    private String surname;

    @NotEmpty
    @Pattern(regexp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
            + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$")
    private String email;

    @Pattern(regexp = "^(?=.{2,32}$)(?![_.])(?!.*[_.]{2})[a-z0-9._]+(?<![_.])$")
    @NotEmpty
    private String username;

    @NotEmpty
    @Size(min = 8, max = 100, message = "Password length must be between 8-100 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\.@$!%*#?&])[A-Za-z\\d\\.@$!%*#?&]{8,}$")
    private String password;

    @NotNull
    private AuthProviderType authProvider;

    boolean emailValidationByCode;

    public User toUser() {
        return User.builder()
                .name(name)
                .surname(surname)
                .username(username)
                .email(email)
                .password(password)
                .authProvider(authProvider)
                .phone("")
                .canChangeUsername(false)
                .activated(true)
                .locked(false)
                .failedLoginCount(0)
                .showCaptcha(false)
                .emailValidated(false)
                .createdDate(LocalDateTime.now())
                .build();
    }


}