package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.data.model.Localization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalizationResponseDto {

    private String languageCode;
    private String key;
    private String message;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public static LocalizationResponseDto fromLocalization(Localization localization){
        return LocalizationResponseDto.builder()
                .languageCode(localization.getLanguageCode())
                .key(localization.getKey())
                .message(localization.getMessage())
                .createdDate(localization.getCreatedDate())
                .updatedDate(localization.getUpdatedDate())
                .build();
    }

}
