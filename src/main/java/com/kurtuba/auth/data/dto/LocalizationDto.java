package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.data.model.Localization;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalizationDto {

    private String id;

    @NotEmpty
    private String languageCode;

    @NotEmpty
    private String key;

    @NotEmpty
    private String message;

}
