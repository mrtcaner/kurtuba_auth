package com.kurtuba.auth.data.dto;

import com.kurtuba.auth.utils.Utils;
import jakarta.validation.constraints.NotEmpty;
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
public class PasswordChangeDto {

    @NotEmpty
    String oldPassword;
    @NotEmpty
    @Size(min = 8, max = 100, message = "Password length must be between 8-100 characters")
    @Pattern(regexp = Utils.PASSWORD_REGEX)
    String newPassword;
    @NotEmpty
    @Size(min = 8, max = 100, message = "Password length must be between 8-100 characters")
    @Pattern(regexp = Utils.PASSWORD_REGEX)
    String repeatNewPassword;
}
