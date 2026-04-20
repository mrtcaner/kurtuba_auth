package com.kurtuba.auth.config;

import com.kurtuba.auth.data.enums.RateLimitPublicApi;
import io.github.bucket4j.Bandwidth;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kurtuba.rate-limit")
public class RateLimitProperties {

    private final Map<String, PublicApiProperties> publicApi = new LinkedHashMap<>();

    public PublicApiProperties getPublicApi(RateLimitPublicApi api) {
        PublicApiProperties properties = publicApi.get(api.getKey());
        if (properties == null) {
            throw new IllegalStateException("Missing rate limit config for " + api.name());
        }
        return properties;
    }

    public String[] getPublicApiPatterns() {
        return Arrays.stream(RateLimitPublicApi.values())
                     .map(this::getPublicApi)
                     .map(PublicApiProperties::getPattern)
                     .toArray(String[]::new);
    }

    @Getter
    @Setter
    public static class PublicApiProperties {
        private String pattern;
        private int capacity;
        private Duration refill;

        public Bandwidth toBandwidth() {
            return Bandwidth.builder()
                            .capacity(capacity)
                            .refillIntervally(capacity, refill)
                            .build();
        }
    }
}
