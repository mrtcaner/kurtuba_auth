package com.kurtuba.auth.data.dto;

import com.kurtuba.auth.utils.Utils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetByCodeDto {

    @NotBlank
    String emailMobile;
    @NotBlank
    String code;
    @NotBlank
    @Size(min = 6, max = 100, message = "Password length must be between 6-100 characters")
    @Pattern(regexp = Utils.PASSWORD_REGEX)
    String newPassword;
    @NotBlank
    @Size(min = 6, max = 100, message = "Password length must be between 6-100 characters")
    @Pattern(regexp = Utils.PASSWORD_REGEX)
    String repeatNewPassword;

    String clientId;
    String clientSecret;
}
