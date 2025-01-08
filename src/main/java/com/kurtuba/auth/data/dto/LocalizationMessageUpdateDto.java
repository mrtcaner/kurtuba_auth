package com.kurtuba.auth.data.dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalizationMessageUpdateDto {

    @NotEmpty
    private String id;

    @NotEmpty
    private String message;

}
