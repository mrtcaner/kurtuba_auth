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
public class AccountActivationDto {

    @NotEmpty
    String emailMobile;
    @NotEmpty
    String code;

    String clientId;
    String clientSecret;
}
