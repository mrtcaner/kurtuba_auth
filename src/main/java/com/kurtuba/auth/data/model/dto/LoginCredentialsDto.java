package com.parafusion.auth.data.model.dto;

import com.parafusion.auth.data.model.ClientType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginCredentialsDto {
    @NotEmpty
    String emailUsername;
    @NotEmpty
    String pass;
    @NotEmpty
    @Enumerated(EnumType.STRING)
    ClientType clientType;
}
