package com.kurtuba.auth.data.enums;


import com.fasterxml.jackson.annotation.JsonCreator;

public enum AuthProviderType {
    KURTUBA, GOOGLE, FACEBOOK, GITHUB;

    @JsonCreator
    public static AuthProviderType create(String value) {
        if (value == null) {
            return null;
        }
        for (AuthProviderType v : values()) {
            if (value.equals(v.name())) {
                return v;
            }
        }
        return null;
    }
}
