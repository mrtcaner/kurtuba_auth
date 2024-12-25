package com.kurtuba.auth.data.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

/**
 * For all clients except web clients with jwt in cookie
 */
@Data
@Builder
public class TokenRefreshRequestDto {

    @NotEmpty
    String accessToken;

    @NotEmpty
    String refreshToken;

    @NotEmpty
    String clientId;

    String clientSecret;
}
