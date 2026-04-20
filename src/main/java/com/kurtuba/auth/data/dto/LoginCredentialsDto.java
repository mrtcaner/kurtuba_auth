package com.kurtuba.auth.data.dto;

import com.kurtuba.auth.utils.annotation.EmailMobile;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginCredentialsDto {
    @EmailMobile
    String emailMobile;

    @NotBlank
    String password;

    String clientId;

    String clientSecret;
}
