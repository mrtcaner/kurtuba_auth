package com.kurtuba.auth.data.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

/**
 *  For web clients with jwt in cookie
 */
@Data
@Builder
public class TokenRefreshWebRequestDto {

    @NotEmpty
    String clientId;
    
    String clientSecret;
}
