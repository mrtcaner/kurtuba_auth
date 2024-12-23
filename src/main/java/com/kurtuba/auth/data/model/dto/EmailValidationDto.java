package com.kurtuba.auth.data.model.dto;

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

    @NotEmpty
    String userMetaChangeId;
    @NotEmpty
    String code;
}
