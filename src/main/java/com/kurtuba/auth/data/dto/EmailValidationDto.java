package com.kurtuba.auth.data.dto;

import com.kurtuba.auth.utils.Utils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailValidationDto {

    @Email(regexp = Utils.EMAIL_REGEX)
    @NotEmpty
    String email;
    @NotEmpty
    String code;
}
