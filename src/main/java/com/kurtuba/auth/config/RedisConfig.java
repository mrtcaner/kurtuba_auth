package com.kurtuba.auth.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "kurtuba.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
// this is only needed if you want to use the redis rate limiter
public class RedisConfig {
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create(RedisURI.builder()
                                          .withHost(redisHost)
                                          .withPort(redisPort)
                                          .build());
    }
}
