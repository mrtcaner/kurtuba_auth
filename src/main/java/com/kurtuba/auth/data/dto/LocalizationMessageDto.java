package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.data.model.LocalizationMessage;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalizationMessageDto {

    private String id;

    @NotEmpty
    private String languageCode;

    @NotEmpty
    private String key;

    @NotEmpty
    private String message;

    public static LocalizationMessageDto fromLocalization(LocalizationMessage localizationMessage){
        return LocalizationMessageDto.builder()
                .id(localizationMessage.getId())
                .languageCode(localizationMessage.getLanguageCode())
                .key(localizationMessage.getKey())
                .message(localizationMessage.getMessage())
                .build();
    }

}
