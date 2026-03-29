package com.kurtuba.auth.data.dto;

import com.kurtuba.auth.data.model.LocalizationAvailableLocale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvailableLocaleDto {
    private String languageCode;
    private String countryCode;

    public static AvailableLocaleDto fromEntity(LocalizationAvailableLocale locale) {
        return AvailableLocaleDto.builder()
                .languageCode(locale.getLanguageCode())
                .countryCode(locale.getCountryCode())
                .build();
    }
}
