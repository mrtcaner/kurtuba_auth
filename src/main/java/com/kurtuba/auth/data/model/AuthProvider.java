package com.parafusion.auth.data.model;


import com.fasterxml.jackson.annotation.JsonCreator;

public enum AuthProvider {
    PARAFUSION, GOOGLE, FACEBOOK, GITHUB;

    @JsonCreator
    public static AuthProvider create(String value) {
        if (value == null) {
            return null;
        }
        for (AuthProvider v : values()) {
            if (value.equals(v.name())) {
                return v;
            }
        }
        return null;
    }
}
