package com.kurtuba.auth.data.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ForgotPasswordDto {

    @NotEmpty
    private String email;
}