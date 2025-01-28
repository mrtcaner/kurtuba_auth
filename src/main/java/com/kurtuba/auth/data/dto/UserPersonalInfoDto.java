package com.kurtuba.auth.data.dto;

import com.kurtuba.auth.data.enums.GenderType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPersonalInfoDto {

    @NotBlank
    String name;

    String surname;
    @Enumerated(EnumType.STRING)
    GenderType gender;
    String birthdate;

}
