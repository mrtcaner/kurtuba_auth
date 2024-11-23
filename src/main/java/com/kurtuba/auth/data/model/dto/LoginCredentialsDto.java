package com.kurtuba.auth.data.model.dto;

import com.kurtuba.auth.data.model.ClientType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class LoginCredentialsDto {
    @NotEmpty String emailUsername;
    @NotEmpty String pass;
    @NotNull
    @Enumerated(EnumType.STRING)
    ClientType clientType;
}
