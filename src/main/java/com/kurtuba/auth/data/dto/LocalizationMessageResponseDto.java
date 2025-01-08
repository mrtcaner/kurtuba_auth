package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.data.model.LocalizationMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalizationMessageResponseDto {

    private String id;
    private String languageCode;
    private String key;
    private String message;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public static LocalizationMessageResponseDto fromLocalization(LocalizationMessage localizationMessage){
        return LocalizationMessageResponseDto.builder()
                .id(localizationMessage.getId())
                .languageCode(localizationMessage.getLanguageCode())
                .key(localizationMessage.getKey())
                .message(localizationMessage.getMessage())
                .createdDate(localizationMessage.getCreatedDate())
                .updatedDate(localizationMessage.getUpdatedDate())
                .build();
    }

}
