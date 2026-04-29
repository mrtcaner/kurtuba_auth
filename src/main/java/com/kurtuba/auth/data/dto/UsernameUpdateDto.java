package com.kurtuba.auth.data.dto;

import com.kurtuba.auth.utils.annotation.UserName;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsernameUpdateDto {

    @NotBlank
    @UserName
    private String username;
}
