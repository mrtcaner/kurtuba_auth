package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.TokenRefreshRequestDto;
import com.kurtuba.auth.data.dto.TokenReturnDto;
import com.kurtuba.auth.data.dto.TokensReturnDto;
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
    public void save(UserToken userToken) {
        userTokenRepository.save(userToken);
    }

    @Transactional
    public TokenReturnDto refreshUserTokens(TokenRefreshRequestDto tokenRefreshRequestDto) {

        // token validation
        JsonObject decodedToken = TokenUtils.decodeTokenPayload(tokenRefreshRequestDto.getAccessToken());
        UserToken userToken = checkRefreshTokenState(decodedToken.get(JWTClaimType.JTI.getDisplayName()).getAsString());
        // token will be verified with the client used for its creation
        RegisteredClient tokenClient = registeredClientRepository.findByClientId(userToken.getClientId());
        // verify signature
        Claims claims = verifyAccessToken(tokenRefreshRequestDto.getAccessToken(),
                Duration.ofMinutes(tokenClient.getRefreshTokenTtlMinutes()).getSeconds());

        // client validation
        RegisteredClient client = registeredClientRepository.findByClientId(tokenRefreshRequestDto.getClientId());
        if (client == null) {
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
        }

        if (StringUtils.hasLength(client.getClientSecret())) {
            if(!StringUtils.hasLength(tokenRefreshRequestDto.getClientSecret())){
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
            if (!new BCryptPasswordEncoder().matches(tokenRefreshRequestDto.getClientSecret(), client.getClientSecret())) {
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
        }

        //Decode refresh token
        byte[] decodedBytes = Base64.getDecoder().decode(tokenRefreshRequestDto.getRefreshToken());
        String decodedRefreshToken = new String(decodedBytes);

        //Compare saved bcrypt hash with received token
        if (!new BCryptPasswordEncoder().matches(decodedRefreshToken, userToken.getRefreshToken())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        User user = userRepository.getUserById(claims.getSubject());
        // check user state
        if (!user.isActivated() || !user.isEmailValidated() || user.isLocked() || user.isShowCaptcha()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

            // delete old tokens
            userTokenRepository.delete(userToken);

            Set<String> roles = null;
            if (client.isScopeEnabled()) {
                roles = user.getUserRoles().stream().map(role -> role.getRole().name()).collect(Collectors.toSet());
            }

            Duration accessTokenTtlMinutes = Duration.ofMinutes(client.getAccessTokenTtlMinutes());
            Duration refreshTokenTtlMinutes = null;
            if (client.isRefreshTokenEnabled()) {
                refreshTokenTtlMinutes = Duration.ofMinutes(client.getRefreshTokenTtlMinutes());
            }
            // then save new tokens
            return createAndSaveTokens(claims.getSubject(), client.getClientId(), Set.of(client.getClientName()), roles,
                    accessTokenTtlMinutes, refreshTokenTtlMinutes);
    }


    @Transactional
    public String refreshWebClientWithCookieTokens(String accessToken, String clientId, String clientSecret) {

        // token validation
        JsonObject decodedToken = TokenUtils.decodeTokenPayload(accessToken);
        UserToken userToken = checkRefreshTokenState(decodedToken.get(JWTClaimType.JTI.getDisplayName()).getAsString());
        // token will be verified with the client used for its creation
        RegisteredClient tokenClient = registeredClientRepository.findByClientId(userToken.getClientId());
        // verify signature
        Claims claims = verifyAccessToken(accessToken, Duration.ofMinutes(tokenClient.getRefreshTokenTtlMinutes()).getSeconds());

        // client validation
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
        }

        if (StringUtils.hasLength(client.getClientSecret())) {
            if(!StringUtils.hasLength(clientSecret)){
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
            if (!new BCryptPasswordEncoder().matches(clientSecret, client.getClientSecret())) {
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
        }

        // only the tokens issued for web-client(s) refreshed here
        if (!RegisteredClientType.WEB.equals(client.getClientType())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID);
        }

        User user = userRepository.getUserById(claims.getSubject());
        // check user state
        if (!user.isActivated() || !user.isEmailValidated() || user.isLocked() || user.isShowCaptcha()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        Set<String> auds = Set.of(client.getClientName());

        // delete old tokens
        userTokenRepository.delete(userToken);
        // save new tokens
        Set<String> roles = null;
        if (client.isScopeEnabled()) {
            roles = user.getUserRoles().stream().map(role -> role.getRole().name()).collect(Collectors.toSet());
        }

        Duration accessTokenTtlMinutes = Duration.ofMinutes(client.getAccessTokenTtlMinutes());
        Duration refreshTokenTtlMinutes = null;
        if (client.isRefreshTokenEnabled()) {
            refreshTokenTtlMinutes = Duration.ofMinutes(client.getRefreshTokenTtlMinutes());
        }

        return createAndSaveTokens(claims.getSubject(), client.getClientId(), auds, roles,
                accessTokenTtlMinutes,
                refreshTokenTtlMinutes)
                .getAccessToken();


    }

    /**
     * TODO
     * This method must not be accessed without user credentials or a valid refresh token.
     * That means token-validation/user-authentication must be carried out inside this method.
     */
    @Transactional
    public TokenReturnDto createAndSaveTokens(String userId, String clientId, Set<String> auds, Set<String> scopes,
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
                .aud(auds.stream().toList())
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

        return newRefreshToken == null ? TokenReturnDto.builder()
                .accessToken(newAccessToken).build() :
                TokensReturnDto.tokensReturnDtoBuilder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
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

    private UserToken checkRefreshTokenState(String jti) {
        UserToken userToken = userTokenRepository.findByJtiAndBlockedAndRefreshTokenExpAfter(
                jti, false, LocalDateTime.now());

        if (userToken == null) {
            throw new BusinessLogicException(ErrorEnum.AUTH_INVALID_TOKEN);
        }

        return userToken;
    }


}
