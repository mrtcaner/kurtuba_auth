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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserTokenService {

    final UserTokenRepository userTokenRepository;

    final TokenUtils tokenUtils;

    final UserRepository userRepository;

    final RegisteredClientRepository registeredClientRepository;

    public UserTokenService(UserTokenRepository userTokenRepository, TokenUtils tokenUtils,
            UserRepository userRepository, RegisteredClientRepository registeredClientRepository) {
        this.userTokenRepository = userTokenRepository;
        this.tokenUtils = tokenUtils;
        this.userRepository = userRepository;
        this.registeredClientRepository = registeredClientRepository;
    }

    public List<UserToken> findAllByUserId(String userId) {
        return userTokenRepository.findAllByUserId(userId);
    }

    public List<UserToken> findAllByUserIdAndBlocked(String userId, boolean blocked) {
        return userTokenRepository.findAllByUserIdAndBlocked(userId, blocked);
    }

    public Map<String, Instant> findLatestCreatedDatesByUserIds(Set<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Map.of();
        }

        return userTokenRepository.findLatestCreatedDatesByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserTokenRepository.UserLatestTokenCreatedDateProjection::getUserId,
                        UserTokenRepository.UserLatestTokenCreatedDateProjection::getLastTokenCreatedDate));
    }

    public Optional<UserToken> findByJTI(String jti) {
        return userTokenRepository.findByJti(jti);
    }

    public Optional<UserToken> findByJTIAndBlocked(String jti, boolean blocked) {
        return userTokenRepository.findByJtiAndBlocked(jti, blocked);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public void changeTokenBlockByJTI(List<String> blockTokens, boolean block) {
        if (block) {
            userTokenRepository.saveAll(blockTokens.stream().map(jti -> {
                UserToken ut = findByJTIAndBlocked(jti, false).orElseThrow(
                        () -> new BusinessLogicException(ErrorEnum.INVALID_PARAMETER.getCode(),
                                "Inconsistent token state"));
                ut.setBlocked(true);
                ut.setUpdatedDate(Instant.now());
                return ut;
            }).toList());
        } else {
            userTokenRepository.saveAll(blockTokens.stream().map(jti -> {
                UserToken ut = findByJTIAndBlocked(jti, true).orElseThrow(
                        () -> new BusinessLogicException(ErrorEnum.INVALID_PARAMETER.getCode(),
                                "Inconsistent token state"));
                ut.setBlocked(false);
                ut.setUpdatedDate(Instant.now());
                return ut;
            }).toList());
        }

    }

    @Transactional
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public void blockUsersTokens(String userId) {
        List<UserToken> usersTokens = findAllByUserId(userId).stream().map(token -> {
            token.setBlocked(true);
            token.setUpdatedDate(Instant.now());
            return token;
        }).toList();
        if (!CollectionUtils.isEmpty(usersTokens))
            userTokenRepository.saveAll(usersTokens);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public boolean checkDBIfTokenIsBlockedByJTI(String jti) {
        return userTokenRepository.findByJtiAndBlocked(jti, true).isPresent();
    }

    @Transactional
    public TokensResponseDto refreshUserTokens(TokenRefreshRequestDto tokenRefreshRequestDto) {
        AccessTokenValidationResult result = validateAccessTokenBeforeTokenRefresh(tokenRefreshRequestDto.getAccessToken(),
                                                                                   tokenRefreshRequestDto.getClientId(),
                                                                                   tokenRefreshRequestDto.getClientSecret());

        // Decode refresh token
        byte[] decodedBytes = Base64.getDecoder().decode(tokenRefreshRequestDto.getRefreshToken());
        String decodedRefreshToken = new String(decodedBytes);

        // Compare saved bcrypt hash with received token
        if (!new BCryptPasswordEncoder().matches(decodedRefreshToken, result.getUserToken().getRefreshToken())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        consumeRefreshToken(result.getUserToken());
        return getTokens(result);
    }

    private TokensResponseDto getTokens(AccessTokenValidationResult result) {
        User user = userRepository.getUserById(result.getClaims().getSubject())
                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        // check user state
        if (!user.isActivated() || user.isLocked() || user.isShowCaptcha() || user.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        // save new tokens
        Set<String> roles = null;
        if (result.getRegisteredClient().isScopeEnabled()) {
            roles = user.getUserRoles()
                    .stream()
                    .filter(userRole -> result.getRegisteredClient().getScopes().contains(userRole.getRole().getName()))
                    .collect(Collectors.toSet())
                    .stream()
                    .map(userRole -> userRole.getRole().getName())
                    .collect(Collectors.toSet());
        }

        Duration accessTokenTtlMinutes = Duration.ofMinutes(result.getRegisteredClient().getAccessTokenTtlMinutes());
        Duration refreshTokenTtlMinutes = null;
        if (result.getRegisteredClient().isRefreshTokenEnabled()) {
            refreshTokenTtlMinutes = Duration.ofMinutes(result.getRegisteredClient().getRefreshTokenTtlMinutes());
        }

        return createAndSaveTokens(result.getClaims().getSubject(), result.getRegisteredClient().getClientId(),
                result.getRegisteredClient().getAuds(), roles, accessTokenTtlMinutes,
                refreshTokenTtlMinutes);
    }

    /**
     * TODO
     * This method must not be accessed without user credentials or a valid refresh
     * token.
     * That means token-validation/user-authentication must be carried out inside
     * this method.
     */
    @Transactional
    public TokensResponseDto createAndSaveTokens(String userId, String clientId, Set<String> auds, Set<String> scopes,
            Duration accessTokenValidityDuration,
            Duration refreshTokenValidityDuration) {

        String newAccessToken = tokenUtils.generateToken(userId, auds, scopes, accessTokenValidityDuration, clientId);
        String newRefreshToken = null;
        if (refreshTokenValidityDuration != null) {
            newRefreshToken = tokenUtils.generateRefreshToken();
        }
        JsonObject decodedNewToken = TokenUtils.decodeTokenPayload(newAccessToken);

        Instant expirationDate = Instant.ofEpochSecond(
                Long.parseLong(decodedNewToken.get(JWTClaimType.EXP.getDisplayName()).getAsString()));

        UserToken newUserToken = UserToken.builder()
                .jti(decodedNewToken.get(JWTClaimType.JTI.getDisplayName()).getAsString())
                .userId(decodedNewToken.get(JWTClaimType.SUB.getDisplayName()).getAsString())
                .clientId(clientId)
                .auds(auds.stream().toList())
                .expirationDate(expirationDate)
                .refreshToken(newRefreshToken != null ? new BCryptPasswordEncoder().encode(
                        new String(Base64.getDecoder().decode(newRefreshToken))) : null)
                .refreshTokenExp(newRefreshToken != null ? Instant.now()
                        .plus(refreshTokenValidityDuration)
                        : null)
                .scopes(CollectionUtils.isEmpty(scopes) ? null : scopes.stream().toList())
                .createdDate(Instant.now())
                .blocked(false)
                .build();

        userTokenRepository.save(newUserToken);

        return TokensResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken == null ? "" : newRefreshToken)
                .build();
    }

    private AccessTokenValidationResult validateAccessTokenBeforeTokenRefresh(String accessToken, String clientId, String clientSecret) {
        // token validation
        JsonObject decodedToken = TokenUtils.decodeTokenPayload(accessToken);
        UserToken userToken = userTokenRepository.findByJti(
                (decodedToken.get(JWTClaimType.JTI.getDisplayName()).getAsString()))
                .orElseThrow(() -> new BusinessLogicException(
                        ErrorEnum.AUTH_ACCESS_TOKEN_INVALID));

        if (userToken.getRefreshTokenExp() == null || userToken.getRefreshTokenExp().isBefore(Instant.now())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        if (userToken.isRefreshTokenUsed()) {
            boolean withinGracePeriod = userToken.getUsedDate() != null &&
                                        !userToken.getUsedDate().isBefore(Instant.now().minusSeconds(30));
            if (!withinGracePeriod) {
                throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_USED);
            }
        }

        if (userToken.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.AUTH_TOKEN_BLOCKED);
        }

        if (!userToken.getClientId().equals(clientId)) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_CLIENT_MISMATCH);
        }

        // token will be verified with the client used for its creation
        RegisteredClient tokenClient = registeredClientRepository.findByClientId(userToken.getClientId())
                .orElseThrow(() -> new BusinessLogicException(
                        ErrorEnum.AUTH_CLIENT_INVALID));
        // verify signature
        Claims claims = verifyAccessToken(accessToken,
                Duration.ofMinutes(tokenClient.getRefreshTokenTtlMinutes()).getSeconds());

        // client validation
        RegisteredClient client = registeredClientRepository.findByClientId(clientId)
                .orElseThrow(() -> new BusinessLogicException(
                        ErrorEnum.AUTH_CLIENT_INVALID));

        if (StringUtils.hasLength(client.getClientSecret())) {
            if (!StringUtils.hasLength(clientSecret)) {
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
            if (!new BCryptPasswordEncoder().matches(clientSecret, client.getClientSecret())) {
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
        }

        return AccessTokenValidationResult.builder()
                .claims(claims)
                .registeredClient(client)
                .userToken(userToken)
                .build();
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

    @Transactional
    public TokensResponseDto refreshWebClientWithCookieTokens(String accessToken, String clientId,
            String clientSecret) {

        AccessTokenValidationResult result = validateAccessTokenBeforeTokenRefresh(accessToken, clientId, clientSecret);

        // only the tokens issued for web-client(s) refreshed here
        if (!RegisteredClientType.WEB.equals(result.getRegisteredClient().getClientType())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID);
        }

        // client does not support refresh
        if (!result.registeredClient.isRefreshTokenEnabled()) {
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID);
        }

        consumeRefreshToken(result.getUserToken());
        return getTokens(result);

    }

    private void consumeRefreshToken(UserToken userToken) {
        // at this point, there is an expired access token with a valid refresh token
        int updatedRows = userTokenRepository.markRefreshTokenAsUsedIfAvailable(userToken.getId(), Instant.now());
        if (updatedRows == 0) {
            // no updates! maybe another thread has updated the token already?
            UserToken freshToken = userTokenRepository.findById(userToken.getId())
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.AUTH_ACCESS_TOKEN_INVALID));

            boolean withinGracePeriod = !freshToken.getUsedDate().isBefore(Instant.now().minusSeconds(30));

            if (withinGracePeriod) {
                return; // Token is used but within the 30 seconds grace period, allow it
            }

            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_USED);
        }
    }

    @Transactional
    public void save(UserToken userToken) {
        userTokenRepository.save(userToken);
    }

    @Transactional
    public TokensResponseDto validateRegisteredClientAndGetTokens(User user, String clientId, String clientSecret) {
        if (user.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.USER_BLOCKED);
        }

        if (StringUtils.hasLength(clientId)) {
            RegisteredClient client = registeredClientRepository.findByClientId(clientId)
                    .orElseThrow(() -> new BusinessLogicException(
                            ErrorEnum.AUTH_CLIENT_INVALID));
            if (StringUtils.hasLength(client.getClientSecret())) {
                if (!StringUtils.hasLength(clientSecret) ||
                        !new BCryptPasswordEncoder().matches(clientSecret, client.getClientSecret())) {
                    throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
                }
            }

            // credentials validated
            // generate tokens and return
            Duration accessTokenValidity = Duration.ofMinutes(client.getAccessTokenTtlMinutes());
            Duration refreshTokenValidity = null;
            if (client.isRefreshTokenEnabled()) {
                refreshTokenValidity = Duration.ofMinutes(client.getRefreshTokenTtlMinutes());
            }

            Set<String> roles = null;
            if (client.isScopeEnabled()) {
                roles = user.getUserRoles()
                        .stream()
                        .filter(userRole -> client.getScopes().contains(userRole.getRole().getName()))
                        .collect(Collectors.toSet())
                        .stream()
                        .map(userRole -> userRole.getRole().getName())
                        .collect(Collectors.toSet());
            }

            // create token(s)
            return createAndSaveTokens(user.getId(), client.getClientId(), client.getAuds(), roles, accessTokenValidity,
                    refreshTokenValidity);

        }

        return null;
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
