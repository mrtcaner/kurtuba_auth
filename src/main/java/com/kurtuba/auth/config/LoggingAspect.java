package com.kurtuba.auth.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kurtuba.auth.data.dto.TokenRefreshRequestDto;
import com.kurtuba.auth.utils.TokenUtils;
import com.nimbusds.jose.shaded.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import java.lang.reflect.Array;
import java.security.Principal;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Aspect
@Component
@ConditionalOnProperty(prefix = "kurtuba.logging.aspect", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);

    private static final Set<String> SENSITIVE_PARAM_KEYWORDS = new HashSet<>(
            Arrays.asList("password", "pass", "pwd", "secret", "accesstoken", "refreshtoken", "idtoken", "clientsecret",
                          "registeredclientsecret", "authorizationcode", "activationcode", "resetcode",
                          "verificationcode", "verificationlink", "linkparam", "jwt", "sid", "fcmtoken",
                          "firebaseinstallationid", "totp", "otp", "pin"));

    private static final Set<Class<?>> SIMPLE_TYPES = new HashSet<>(
            Arrays.asList(String.class, Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class,
                          Double.class, Character.class));

    private static final String MASKED = "***";

    private final ObjectMapper objectMapper;

    public LoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("(execution(* com.kurtuba..service..*(..)) || execution(* com.kurtuba..controller..*(..))) && " +
            "!within(com.kurtuba.auth.scheduled..*) && " +
            "!execution(* com.kurtuba.auth.service.MessageJobService.findByStateAndContactTypeAndSendAfterDateBefore(" +
            "..)) && " +
            "!execution(* com.kurtuba.auth.service.MessageJobService" +
            ".findByStateAndContactTypeAndMessageServiceProviderTypeAndSendAfterDateBefore(..))")
    public Object logMethodCall(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();

        long startNanos = System.nanoTime();
        try {
            return proceedingJoinPoint.proceed();
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            String params = buildParamsLog(methodSignature, proceedingJoinPoint.getArgs());
            LOGGER.info("Method {}.{} executed in {} ms | userId={} | params: {}", className, methodName, durationMs,
                        resolveUserId(proceedingJoinPoint.getArgs()), params);
        }
    }

    private String buildParamsLog(MethodSignature methodSignature, Object[] args) {
        String[] paramNames = methodSignature.getParameterNames();
        if (args == null || args.length == 0) {
            return "{}";
        }

        ObjectNode paramsNode = objectMapper.createObjectNode();
        for (int i = 0; i < args.length; i++) {
            String paramName = resolveParamName(paramNames, i);
            Object arg = args[i];
            paramsNode.set(paramName, toLogNode(paramName, arg));
        }
        return paramsNode.toString();
    }

    private String resolveParamName(String[] paramNames, int index) {
        if (paramNames == null || index >= paramNames.length || paramNames[index] == null ||
            paramNames[index].isBlank()) {
            return "arg" + index;
        }
        return paramNames[index];
    }

    JsonNode toLogNode(String key, Object value) {
        if (value == null) {
            return objectMapper.nullNode();
        }

        if (isSensitiveKey(key)) {
            return objectMapper.getNodeFactory().textNode(MASKED);
        }

        if (value instanceof HttpServletRequest request) {
            return summarizeRequest(request);
        }

        if (value instanceof HttpServletResponse) {
            return objectMapper.getNodeFactory().textNode("HttpServletResponse");
        }

        if (value instanceof BindingResult bindingResult) {
            return summarizeBindingResult(bindingResult);
        }

        if (value instanceof Principal || value instanceof Authentication || value instanceof Jwt) {
            return summarizePrincipal(value);
        }

        if (isSimpleValue(value)) {
            return objectMapper.valueToTree(value);
        }

        if (value.getClass().isArray()) {
            return arrayToNode(key, value);
        }

        if (value instanceof Iterable<?> iterable) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (Object item : iterable) {
                arrayNode.add(toLogNode(key, item));
            }
            return arrayNode;
        }

        if (value instanceof Map<?, ?> map) {
            ObjectNode mapNode = objectMapper.createObjectNode();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String entryKey = String.valueOf(entry.getKey());
                mapNode.set(entryKey, toLogNode(entryKey, entry.getValue()));
            }
            return mapNode;
        }

        try {
            JsonNode objectNode = objectMapper.valueToTree(value);
            maskSensitiveFields(objectNode);
            return objectNode;
        } catch (IllegalArgumentException ex) {
            return objectMapper.getNodeFactory().textNode(safeToString(value));
        }
    }

    private ArrayNode arrayToNode(String key, Object value) {
        int length = Array.getLength(value);
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (int i = 0; i < length; i++) {
            arrayNode.add(toLogNode(key, Array.get(value, i)));
        }
        return arrayNode;
    }

    private ObjectNode summarizeRequest(HttpServletRequest request) {
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("type", "HttpServletRequest");
        requestNode.put("method", request.getMethod());
        requestNode.put("requestUri", request.getRequestURI());
        if (request.getQueryString() != null) {
            requestNode.put("queryString", request.getQueryString());
        }
        return requestNode;
    }

    private ObjectNode summarizeBindingResult(BindingResult bindingResult) {
        ObjectNode bindingNode = objectMapper.createObjectNode();
        bindingNode.put("type", "BindingResult");
        bindingNode.put("errorCount", bindingResult.getErrorCount());
        ArrayNode fieldErrors = bindingNode.putArray("fieldErrors");
        bindingResult.getFieldErrors().forEach(fieldError -> fieldErrors.add(fieldError.getField()));
        return bindingNode;
    }

    private ObjectNode summarizePrincipal(Object value) {
        ObjectNode principalNode = objectMapper.createObjectNode();
        principalNode.put("type", value.getClass().getSimpleName());
        principalNode.put("userId", resolveUserId(new Object[]{value}));
        return principalNode;
    }

    private String resolveUserId(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                String userId = extractUserId(arg);
                if (userId != null) {
                    return userId;
                }
            }
        }

        String securityContextUserId = extractUserId(SecurityContextHolder.getContext().getAuthentication());
        return securityContextUserId != null ? securityContextUserId : "n/a";
    }

    private String extractUserId(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Jwt jwt) {
            return normalizeUserId(jwt.getSubject());
        }

        if (value instanceof Authentication authentication) {
            if (!authentication.isAuthenticated()) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt) {
                return normalizeUserId(jwt.getSubject());
            }
            return normalizeUserId(authentication.getName());
        }

        if (value instanceof Principal principal) {
            return normalizeUserId(principal.getName());
        }

        if (value instanceof TokenRefreshRequestDto) {
            JsonObject jwtJson = TokenUtils.decodeTokenPayload(((TokenRefreshRequestDto) value).getAccessToken());
            return normalizeUserId(jwtJson.get("sub").getAsString());
        }

        return null;
    }

    private String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank() || "anonymousUser".equalsIgnoreCase(userId)) {
            return null;
        }
        return userId;
    }

    private void maskSensitiveFields(JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveKey(field.getKey())) {
                    if (field.getKey().equalsIgnoreCase("accesstoken") &&
                        StringUtils.hasLength(field.getValue().textValue())) {
                        objectNode.put(field.getKey(), field.getValue()
                                                            .textValue()
                                                            .substring(field.getValue().textValue().length() - 10));
                    } else {
                        objectNode.put(field.getKey(), MASKED);
                    }

                } else {
                    maskSensitiveFields(field.getValue());
                }
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                maskSensitiveFields(child);
            }
        }
    }

    boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (String sensitiveKeyword : SENSITIVE_PARAM_KEYWORDS) {
            String normalizedKeyword = sensitiveKeyword.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (normalized.contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSimpleValue(Object value) {
        Class<?> type = value.getClass();
        return type.isPrimitive() || SIMPLE_TYPES.contains(type) || Number.class.isAssignableFrom(type) ||
               Enum.class.isAssignableFrom(type) || Temporal.class.isAssignableFrom(type) ||
               type.getPackageName().startsWith("java.time") || UUID.class.isAssignableFrom(type);
    }

    private String safeToString(Object value) {
        String className = value.getClass().getSimpleName();
        if (isSensitiveKey(className)) {
            return MASKED;
        }

        try {
            return value.toString();
        } catch (Exception ex) {
            return className;
        }
    }
}
