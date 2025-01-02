package com.kurtuba.auth.data.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
public class TokensResponseDto extends TokenResponseDto {

    @NotEmpty
    public String refreshToken;

    @Builder(builderMethodName = "tokensReturnDtoBuilder")
    public TokensResponseDto(String accessToken, String refreshToken) {
        super(accessToken);
        this.refreshToken = refreshToken;
    }
}
