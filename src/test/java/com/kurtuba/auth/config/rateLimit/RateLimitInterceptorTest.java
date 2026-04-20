package com.kurtuba.auth.config.rateLimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

    @Test
    void shouldPreferForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Forwarded", "for=198.51.100.24;proto=https;host=api.kurtuba.app");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");
        request.setRemoteAddr("10.0.0.2");

        assertThat(RateLimitInterceptor.getClientIp(request)).isEqualTo("198.51.100.24");
    }

    @Test
    void shouldUseFirstXForwardedForValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");
        request.setRemoteAddr("10.0.0.2");

        assertThat(RateLimitInterceptor.getClientIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void shouldFallbackToXRealIpWhenNeeded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "203.0.113.11");
        request.setRemoteAddr("10.0.0.2");

        assertThat(RateLimitInterceptor.getClientIp(request)).isEqualTo("203.0.113.11");
    }

    @Test
    void shouldFallbackToRemoteAddrWhenHeadersMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");

        assertThat(RateLimitInterceptor.getClientIp(request)).isEqualTo("10.0.0.2");
    }
}
