package com.kurtuba.auth.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsersLocalesResponseDto {
    Map<String, UserLocaleDto> locales;
}
