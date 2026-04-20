package com.kurtuba.auth.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSettingLocaleDto {
    String id;
    String languageCode;
    String countryCode;
    Instant createdDate;
    Instant updatedDate;
}
