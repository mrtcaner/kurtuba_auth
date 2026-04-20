package com.kurtuba.auth.data.enums;

public enum RateLimitPublicApi {
    REGISTRATION("registration"),
    LOGIN("login"),
    TOKEN_REFRESH("token-refresh"),
    WEB_TOKEN_REFRESH("web-token-refresh"),
    PASSWORD_RESET("password-reset"),
    SMS("sms"),
    VERIFICATION("verification");

    private final String key;

    RateLimitPublicApi(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
