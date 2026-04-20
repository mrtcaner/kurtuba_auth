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
public class AccountActivationDto {

    @EmailMobile
    String emailMobile;
    @NotBlank
    String code;

    String clientId;
    String clientSecret;
}
