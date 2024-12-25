package com.kurtuba.auth.data.dto;

import com.kurtuba.auth.data.enums.AuthProviderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationOtherProviderDto {

    @NotNull
    AuthProviderType provider;
    @NotEmpty
    String clientId;
    @NotEmpty
    String token;
}
