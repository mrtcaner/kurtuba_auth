package com.kurtuba.auth.data.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginCredentialsDto {
    @NotEmpty
    String emailUsername;

    @NotEmpty
    String password;

    String clientId;

    String clientSecret;
}
