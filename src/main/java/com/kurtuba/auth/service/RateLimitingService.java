package com.kurtuba.auth.service;

import com.kurtuba.auth.config.RateLimitProperties;
import com.kurtuba.auth.data.enums.RateLimitPublicApi;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kurtuba.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitingService {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties rateLimitProperties;

    public Bucket resolveBucket(String ip, RateLimitPublicApi api) {
        // Sanitize IP (IPv6 safe)
        String identifier = ip.replace(":", "-");
        String key = "rl:v1:" + api.name() + ":" + identifier;

        return proxyManager.builder().build(key, () ->
                BucketConfiguration.builder()
                                   .addLimit(rateLimitProperties.getPublicApi(api).toBandwidth())
                                   .build()
        );
    }
}
