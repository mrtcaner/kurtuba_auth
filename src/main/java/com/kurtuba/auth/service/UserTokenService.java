package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.TokenRefreshRequestDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.enums.JWTClaimType;
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
import com.nimbusds.jose.shaded.gson.JsonObject;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserTokenService {

    final
    UserTokenRepository userTokenRepository;

    final
    TokenUtils tokenUtils;

    final
    UserRepository userRepository;

    final RegisteredClientRepository registeredClientRepository;

    public UserTokenService(UserTokenRepository userTokenRepository, TokenUtils tokenUtils,
                            UserRepository userRepository, RegisteredClientRepository registeredClientRepository) {
        this.userTokenRepository = userTokenRepository;
        this.tokenUtils = tokenUtils;
        this.userRepository = userRepository;
        this.registeredClientRepository = registeredClientRepository;
    }

    @Transactional
    public TokensResponseDto refreshUserTokens(TokenRefreshRequestDto tokenRefreshRequestDto) {
        AccessTokenValidationResult result = validateAccessToken(tokenRefreshRequestDto.getAccessToken(), tokenRefreshRequestDto.getClientId(),
                tokenRefreshRequestDto.getClientSecret());

        //Decode refresh token
        byte[] decodedBytes = Base64.getDecoder().decode(tokenRefreshRequestDto.getRefreshToken());
        String decodedRefreshToken = new String(decodedBytes);

        //Compare saved bcrypt hash with received token
        if (!new BCryptPasswordEncoder().matches(decodedRefreshToken, result.getUserToken().getRefreshToken())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        return getTokens(result);
    }

    private TokensResponseDto getTokens(AccessTokenValidationResult result){
        User user = userRepository.getUserById(result.getClaims().getSubject()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        // check user state
        if (!user.isEmailVerified()) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_NOT_VERIFIED);
        }

        if (!user.isActivated() || user.isLocked() || user.isShowCaptcha()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        // delete old tokens
        userTokenRepository.delete(result.getUserToken());
        // save new tokens
        Set<String> roles = null;
        if (result.getRegisteredClient().isScopeEnabled()) {
            roles = user.getUserRoles().stream().map(role -> role.getRole().name()).collect(Collectors.toSet());
        }

        Duration accessTokenTtlMinutes = Duration.ofMinutes(result.getRegisteredClient().getAccessTokenTtlMinutes());
        Duration refreshTokenTtlMinutes = null;
        if (result.getRegisteredClient().isRefreshTokenEnabled()) {
            refreshTokenTtlMinutes = Duration.ofMinutes(result.getRegisteredClient().getRefreshTokenTtlMinutes());
        }

        return createAndSaveTokens(result.getClaims().getSubject(), result.getRegisteredClient().getClientId(),
                Set.of(result.getRegisteredClient().getClientName()), roles,
                accessTokenTtlMinutes, refreshTokenTtlMinutes);
    }

    /**
     * TODO
     * This method must not be accessed without user credentials or a valid refresh token.
     * That means token-validation/user-authentication must be carried out inside this method.
     */
    @Transactional
    public TokensResponseDto createAndSaveTokens(String userId, String clientId, Set<String> auds, Set<String> scopes,
                                                 Duration accessTokenValidityDuration, Duration refreshTokenValidityDuration) {

        String newAccessToken = tokenUtils.generateToken(userId, auds, scopes, accessTokenValidityDuration);
        String newRefreshToken = null;
        if (refreshTokenValidityDuration != null) {
            newRefreshToken = tokenUtils.generateRefreshToken();
        }
        JsonObject decodedNewToken = TokenUtils.decodeTokenPayload(newAccessToken);

        Instant instant = Instant.ofEpochSecond(Long.parseLong(decodedNewToken.get(JWTClaimType.EXP.getDisplayName()).getAsString()));
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime expirationDate = instant.atZone(zoneId).toLocalDateTime();

        UserToken newUserToken = UserToken.builder()
                .jti(decodedNewToken.get(JWTClaimType.JTI.getDisplayName()).getAsString())
                .userId(decodedNewToken.get(JWTClaimType.SUB.getDisplayName()).getAsString())
                .clientId(clientId)
                .auds(auds.stream().toList())
                .expirationDate(expirationDate)
                .refreshToken(newRefreshToken != null ?
                        new BCryptPasswordEncoder().encode(new String(Base64.getDecoder().decode(newRefreshToken))) :
                        null)
                .refreshTokenExp(newRefreshToken != null ? LocalDateTime.now().plus(refreshTokenValidityDuration) :
                        null)
                .scopes(CollectionUtils.isEmpty(scopes) ? null : scopes.stream().toList())
                .createdDate(LocalDateTime.now())
                .blocked(false)
                .build();

        userTokenRepository.save(newUserToken);

        return TokensResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken == null ? "" : newRefreshToken)
                .build();
    }

    private AccessTokenValidationResult validateAccessToken(String accessToken, String clientId, String clientSecret) {
        // token validation
        JsonObject decodedToken = TokenUtils.decodeTokenPayload(accessToken);
        UserToken userToken = checkRefreshTokenStateAndGet(decodedToken.get(JWTClaimType.JTI.getDisplayName()).getAsString())
                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.AUTH_INVALID_TOKEN));
        // token will be verified with the client used for its creation
        RegisteredClient tokenClient = registeredClientRepository.findByClientId(userToken.getClientId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID));
        // verify signature
        Claims claims = verifyAccessToken(accessToken, Duration.ofMinutes(tokenClient.getRefreshTokenTtlMinutes()).getSeconds());

        // client validation
        RegisteredClient client = registeredClientRepository.findByClientId(clientId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID));

        if (StringUtils.hasLength(client.getClientSecret())) {
            if (!StringUtils.hasLength(clientSecret)) {
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
            if (!new BCryptPasswordEncoder().matches(clientSecret, client.getClientSecret())) {
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
        }

        return AccessTokenValidationResult.builder().claims(claims).registeredClient(client).userToken(userToken).build();
    }

    private Claims verifyAccessToken(String accessToken, long clockSkew) {
        Claims claims;
        try {
            claims = tokenUtils.getVerifiedTokenClaims(accessToken, clockSkew);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID, e);
        }

        return claims;
    }

    private Optional<UserToken> checkRefreshTokenStateAndGet(String jti) {
        return userTokenRepository.findByJtiAndBlockedAndRefreshTokenExpAfter(
                jti, false, LocalDateTime.now());
    }

    @Transactional
    public TokensResponseDto refreshWebClientWithCookieTokens(String accessToken, String clientId, String clientSecret) {

        AccessTokenValidationResult result = validateAccessToken(accessToken, clientId, clientSecret);

        // only the tokens issued for web-client(s) refreshed here
        if (!RegisteredClientType.WEB.equals(result.getRegisteredClient().getClientType())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID);
        }

        return getTokens(result);

    }

    @Transactional
    public void save(UserToken userToken) {
        userTokenRepository.save(userToken);
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
class AccessTokenValidationResult {
    Claims claims;
    RegisteredClient registeredClient;
    UserToken userToken;
}
