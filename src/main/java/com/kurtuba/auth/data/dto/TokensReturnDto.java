package com.kurtuba.auth.data.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
public class TokensReturnDto extends TokenReturnDto {

    @NotEmpty
    public String refreshToken;

    @Builder(builderMethodName = "tokensReturnDtoBuilder")
    public TokensReturnDto(String accessToken, String refreshToken) {
        super(accessToken);
        this.refreshToken = refreshToken;
    }
}
