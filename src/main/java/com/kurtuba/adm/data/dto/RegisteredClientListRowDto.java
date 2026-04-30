package com.kurtuba.adm.data.dto;

import com.kurtuba.auth.data.enums.RegisteredClientType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisteredClientListRowDto {
    private final String id;
    private final String clientId;
    private final String clientName;
    private final RegisteredClientType clientType;
    private final String scopesDisplay;
    private final int accessTokenTtlMinutes;
}
