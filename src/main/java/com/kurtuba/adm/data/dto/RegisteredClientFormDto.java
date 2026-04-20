package com.kurtuba.adm.data.dto;

import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredClientFormDto {

    private String id;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientName;

    private String rawClientSecret;

    @NotNull
    private RegisteredClientType clientType;

    private String audsText;

    private boolean scopeEnabled;

    private String scopesText;

    private String redirectUrlsText;

    private String postLogoutRedirectUrlsText;

    @Min(1)
    private int accessTokenTtlMinutes;

    private boolean refreshTokenEnabled;

    @Min(0)
    private int refreshTokenTtlMinutes;

    private boolean sendTokenInCookie;

    private boolean cookieHttpOnly;

    private boolean cookieSecure;

    @Min(0)
    private int cookieMaxAgeSeconds;

    private Instant createdDate;

    public static RegisteredClientFormDto fromRegisteredClient(RegisteredClient client) {
        return RegisteredClientFormDto.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .clientType(client.getClientType())
                .audsText(joinSet(client.getAuds()))
                .scopeEnabled(client.isScopeEnabled())
                .scopesText(joinSet(client.getScopes()))
                .redirectUrlsText(joinSet(client.getRedirectUrls()))
                .postLogoutRedirectUrlsText(joinSet(client.getPostLogoutRedirectUrls()))
                .accessTokenTtlMinutes(client.getAccessTokenTtlMinutes())
                .refreshTokenEnabled(client.isRefreshTokenEnabled())
                .refreshTokenTtlMinutes(client.getRefreshTokenTtlMinutes())
                .sendTokenInCookie(client.isSendTokenInCookie())
                .cookieHttpOnly(client.isCookieHttpOnly())
                .cookieSecure(client.isCookieSecure())
                .cookieMaxAgeSeconds(client.getCookieMaxAgeSeconds())
                .createdDate(client.getCreatedDate())
                .build();
    }

    public RegisteredClient toRegisteredClient(String encodedSecret) {
        return RegisteredClient.builder()
                .id(id)
                .clientId(clientId.trim())
                .clientName(clientName.trim())
                .clientSecret(encodedSecret)
                .clientType(clientType)
                .auds(splitToSet(audsText))
                .scopeEnabled(scopeEnabled)
                .scopes(splitToSet(scopesText))
                .redirectUrls(splitToSet(redirectUrlsText))
                .postLogoutRedirectUrls(splitToSet(postLogoutRedirectUrlsText))
                .accessTokenTtlMinutes(accessTokenTtlMinutes)
                .refreshTokenEnabled(refreshTokenEnabled)
                .refreshTokenTtlMinutes(refreshTokenTtlMinutes)
                .sendTokenInCookie(sendTokenInCookie)
                .cookieHttpOnly(cookieHttpOnly)
                .cookieSecure(cookieSecure)
                .cookieMaxAgeSeconds(cookieMaxAgeSeconds)
                .createdDate(createdDate)
                .build();
    }

    private static String joinSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return values.stream()
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    private static Set<String> splitToSet(String values) {
        if (values == null || values.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(values.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
