package com.kurtuba.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingAspectTest {

    private final LoggingAspect loggingAspect = new LoggingAspect(new ObjectMapper());

    @Test
    void sensitiveKeyListCoversActualSecretFields() {
        assertTrue(loggingAspect.isSensitiveKey("clientSecret"));
        assertTrue(loggingAspect.isSensitiveKey("registeredClientSecret"));
        assertTrue(loggingAspect.isSensitiveKey("authorizationCode"));
        assertTrue(loggingAspect.isSensitiveKey("accessToken"));
        assertTrue(loggingAspect.isSensitiveKey("refreshToken"));
        assertTrue(loggingAspect.isSensitiveKey("idToken"));
        assertTrue(loggingAspect.isSensitiveKey("linkParam"));
        assertTrue(loggingAspect.isSensitiveKey("verificationCode"));
        assertTrue(loggingAspect.isSensitiveKey("fcmToken"));
        assertTrue(loggingAspect.isSensitiveKey("firebaseInstallationId"));
        assertTrue(loggingAspect.isSensitiveKey("sid"));
    }

    @Test
    void sensitiveKeyListDoesNotMaskSafeCodeSuffixes() {
        assertFalse(loggingAspect.isSensitiveKey("verificationByCode"));
        assertFalse(loggingAspect.isSensitiveKey("languageCode"));
        assertFalse(loggingAspect.isSensitiveKey("countryCode"));
        assertFalse(loggingAspect.isSensitiveKey("clientId"));
    }

    @Test
    void requestObjectsAreSummarizedInsteadOfSerialized() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/web/token");
        request.setQueryString("clientId=web-client");

        var node = loggingAspect.toLogNode("request", request);

        assertEquals("HttpServletRequest", node.get("type").asText());
        assertEquals("POST", node.get("method").asText());
        assertEquals("/auth/web/token", node.get("requestUri").asText());
    }

    @Test
    void bindingResultIsSummarizedWithoutDumpingFullObjectGraph() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new TestForm(), "testForm");
        bindingResult.addError(new FieldError("testForm", "email", "bad email"));

        var node = loggingAspect.toLogNode("bindingResult", bindingResult);

        assertEquals("BindingResult", node.get("type").asText());
        assertEquals(1, node.get("errorCount").asInt());
        assertEquals("email", node.get("fieldErrors").get(0).asText());
    }

    private static final class TestForm {
        @SuppressWarnings("unused")
        private String email;
    }
}
