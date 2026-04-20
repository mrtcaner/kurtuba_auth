package com.kurtuba.auth.config.rateLimit;

import com.kurtuba.auth.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kurtuba.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Bucket4jRateLimitConfigurer implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final RateLimitProperties rateLimitProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(rateLimitProperties.getPublicApiPatterns())
                // Always a good idea to exclude static assets just in case
                .excludePathPatterns("/favicon.ico", "/error", "/static/**");
    }
}
