package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.TokenRefreshRequestDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserToken;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.data.repository.UserRepository;
import com.kurtuba.auth.data.repository.UserTokenRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.TokenUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTokenServiceTest {

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private TokenUtils tokenUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @InjectMocks
    private UserTokenService userTokenService;

    @Test
    void validateRegisteredClientAndGetTokens_rejectsBlockedUsers() {
        User user = User.builder()
                .id("user-1")
                .blocked(true)
                .build();

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userTokenService.validateRegisteredClientAndGetTokens(user, "client", "secret"));

        assertEquals(ErrorEnum.USER_BLOCKED.getCode(), ex.getErrorCode());
        verifyNoInteractions(registeredClientRepository, userTokenRepository, tokenUtils, userRepository);
    }

    @Test
    void refreshUserTokens_rejectsExpiredRefreshTokens() {
        Instant now = Instant.now();
        UserToken existingToken = baseUserToken(now)
                .refreshTokenExp(now.minusSeconds(1))
                .build();

        when(userTokenRepository.findByJti("access-jti")).thenReturn(Optional.of(existingToken));

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userTokenService.refreshUserTokens(refreshRequest(accessToken("access-jti"), "refresh-secret")));

        assertEquals(ErrorEnum.AUTH_REFRESH_TOKEN_EXPIRED.getCode(), ex.getErrorCode());

        verify(userTokenRepository, never()).markRefreshTokenAsUsedIfAvailable(any(), any());
    }

    @Test
    void refreshUserTokens_allowsUsedRefreshTokenWithinGracePeriod() {
        Instant now = Instant.now();
        String incomingRefreshToken = "refresh-secret";
        String currentAccessToken = accessToken("access-jti");
        String newAccessToken = accessToken("new-jti", "user-1", now.plusSeconds(300));
        UserToken usedToken = baseUserToken(now)
                .refreshTokenUsed(true)
                .usedDate(now.minusSeconds(10))
                .refreshToken(hashOf(incomingRefreshToken))
                .build();

        RegisteredClient client = refreshEnabledClient();
        User user = activeUser();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");

        when(userTokenRepository.findByJti("access-jti")).thenReturn(Optional.of(usedToken));
        when(userTokenRepository.markRefreshTokenAsUsedIfAvailable(eq("token-1"), any())).thenReturn(0);
        when(userTokenRepository.findById("token-1")).thenReturn(Optional.of(usedToken));
        when(userTokenRepository.save(any(UserToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registeredClientRepository.findByClientId("client-1")).thenReturn(Optional.of(client));
        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(tokenUtils.getVerifiedTokenClaims(currentAccessToken, 3600L)).thenReturn(claims);
        when(tokenUtils.generateToken(eq("user-1"), eq(Set.of("aud")), eq(null), any(), eq("client-1")))
                .thenReturn(newAccessToken);
        when(tokenUtils.generateRefreshToken()).thenReturn(base64("new-refresh-secret"));

        TokensResponseDto response = userTokenService.refreshUserTokens(refreshRequest(currentAccessToken, incomingRefreshToken));

        assertEquals(newAccessToken, response.getAccessToken());
        assertTrue(!response.getRefreshToken().isBlank());

        verify(userTokenRepository).markRefreshTokenAsUsedIfAvailable(eq("token-1"), any());
        verify(userTokenRepository).findById("token-1");
        verify(userTokenRepository).save(any(UserToken.class));
    }

    @Test
    void refreshUserTokens_rejectsUsedRefreshTokenAfterGracePeriod() {
        Instant now = Instant.now();
        UserToken usedToken = baseUserToken(now)
                .refreshTokenUsed(true)
                .usedDate(now.minusSeconds(31))
                .build();

        when(userTokenRepository.findByJti("access-jti")).thenReturn(Optional.of(usedToken));

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userTokenService.refreshUserTokens(refreshRequest(accessToken("access-jti"), "refresh-secret")));

        assertEquals(ErrorEnum.AUTH_REFRESH_TOKEN_USED.getCode(), ex.getErrorCode());

        verify(userTokenRepository, never()).markRefreshTokenAsUsedIfAvailable(any(), any());
    }

    private static TokenRefreshRequestDto refreshRequest(String accessToken, String rawRefreshToken) {
        return TokenRefreshRequestDto.builder()
                .accessToken(accessToken)
                .refreshToken(base64(rawRefreshToken))
                .clientId("client-1")
                .build();
    }

    private static UserToken.UserTokenBuilder baseUserToken(Instant now) {
        return UserToken.builder()
                .id("token-1")
                .userId("user-1")
                .jti("access-jti")
                .clientId("client-1")
                .auds(List.of("aud"))
                .refreshToken(hashOf("refresh-secret"))
                .refreshTokenExp(now.plusSeconds(300))
                .refreshTokenUsed(false)
                .blocked(false)
                .createdDate(now.minusSeconds(60))
                .expirationDate(now.plusSeconds(60));
    }

    private static RegisteredClient refreshEnabledClient() {
        return RegisteredClient.builder()
                .id("rc-1")
                .clientId("client-1")
                .clientName("client")
                .clientType(RegisteredClientType.MOBILE)
                .auds(Set.of("aud"))
                .scopeEnabled(false)
                .accessTokenTtlMinutes(5)
                .refreshTokenEnabled(true)
                .refreshTokenTtlMinutes(60)
                .build();
    }

    private static User activeUser() {
        return User.builder()
                .id("user-1")
                .activated(true)
                .locked(false)
                .showCaptcha(false)
                .blocked(false)
                .userRoles(List.of())
                .build();
    }

    private static String accessToken(String jti) {
        return accessToken(jti, "user-1", Instant.now().plusSeconds(300));
    }

    private static String accessToken(String jti, String sub, Instant exp) {
        String header = base64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64("{\"jti\":\"" + jti + "\",\"sub\":\"" + sub + "\",\"exp\":\"" + exp.getEpochSecond() + "\"}");
        return header + "." + payload + ".signature";
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String hashOf(String value) {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(value);
    }
}
