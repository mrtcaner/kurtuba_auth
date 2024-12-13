package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.ClientType;
import com.kurtuba.auth.data.model.JWTClaimsEnum;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserToken;
import com.kurtuba.auth.data.model.dto.TokensDto;
import com.kurtuba.auth.data.repository.UserRepository;
import com.kurtuba.auth.data.repository.UserTokenRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.TokenUtils;
import com.nimbusds.jose.shaded.gson.JsonObject;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserTokenService {

    @Value("${kurtuba.mobile-client.access-token-validity.minutes}")
    private int mobileClientAccessTokenValidityMinutes;
    @Value("${kurtuba.mobile-client.refresh-token-validity.minutes}")
    private int mobileClientRefreshTokenValidityMinutes;
    @Value("${kurtuba.web-client.access-token-validity.minutes}")
    private int webClientAccessTokenValidityMinutes;
    @Value("${kurtuba.web-client.refresh-token-validity.minutes}")
    private int webClientRefreshTokenValidityMinutes;

    final
    UserTokenRepository userTokenRepository;

    final
    TokenUtils tokenUtils;

    final
    UserRepository userRepository;

    public UserTokenService(UserTokenRepository userTokenRepository, TokenUtils tokenUtils, UserRepository userRepository) {
        this.userTokenRepository = userTokenRepository;
        this.tokenUtils = tokenUtils;
        this.userRepository = userRepository;
    }

    @Transactional
    public void save(UserToken userToken){
        userTokenRepository.save(userToken);
    }

    @Transactional
    public TokensDto refreshUserTokens(TokensDto tokensDto){
        Claims claims = verifyAccessToken(tokensDto.getAccessToken(),Duration.ofMinutes(mobileClientRefreshTokenValidityMinutes).getSeconds());
        UserToken userToken = checkRefreshTokenState(claims.getId().toString());

        //Decode refresh token
        byte[] decodedBytes = Base64.getDecoder().decode(tokensDto.getRefreshToken());
        String decodedRefreshToken = new String(decodedBytes);

        //Compare saved bcrypt hash with received token
        if (!new BCryptPasswordEncoder().matches(decodedRefreshToken, userToken.getRefreshToken())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        User user = userRepository.getUserById(claims.getSubject());
        // check user state
        if(user.isActivated() && user.isEmailValidated() && !user.isLocked() && !user.isShowCaptcha()){
            // delete old tokens
            userTokenRepository.delete(userToken);
            // save new tokens
            return createAndSaveTokens(claims.getSubject(),claims.getAudience().stream()
                    .map(aud-> ClientType.fromClientTypeName(aud)).collect(Collectors.toSet()),
                    Duration.ofMinutes(mobileClientAccessTokenValidityMinutes), Duration.ofMinutes(mobileClientRefreshTokenValidityMinutes));
        }
        throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
    }

    @Transactional
    public String refreshWebClientTokens(String accessToken){
        Claims claims = verifyAccessToken(accessToken,Duration.ofMinutes(webClientRefreshTokenValidityMinutes).getSeconds());
        // only the tokens issued for web-client(s) refreshed here
        // check each aud element to see if any contains "web-client"
        if(claims.getAudience().stream().filter(s -> s.contains("web-client")).findFirst().orElse(null) == null){
           throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }
        UserToken userToken = checkRefreshTokenState(claims.getId().toString());

        User user = userRepository.getUserById(claims.getSubject());
        // check user state
        if(user.isActivated() && user.isEmailValidated() && !user.isLocked() && !user.isShowCaptcha()){
            Set<ClientType> auds = claims.getAudience().stream()
                    .map(aud-> ClientType.fromClientTypeName(aud)).collect(Collectors.toSet());
            if(auds.contains(null)){
                throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
            }

            // delete old tokens
            userTokenRepository.delete(userToken);
            // save new tokens
            return createAndSaveTokens(claims.getSubject(),auds,
                    Duration.ofMinutes(webClientAccessTokenValidityMinutes), Duration.ofMinutes(webClientRefreshTokenValidityMinutes))
                    .getAccessToken();
        }
        throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
    }

    /**
     * TODO
     * This method must not be accessed without user credentials or a valid refresh token.
     * That means token-validation/user-authentication must be carried out inside this method.
     */
    @Transactional
    public TokensDto createAndSaveTokens(String userId, Set<ClientType> clientTypes, Duration accessTokenValidityDuration, Duration refreshTokenValidityDuration){
        String newAccessToken = tokenUtils.generateToken(userId, clientTypes, accessTokenValidityDuration);
        String newRefreshToken = tokenUtils.generateRefreshToken();

        JsonObject decodedNewToken = TokenUtils.decodeTokenPayload(newAccessToken);

        Instant instant = Instant.ofEpochSecond(Long.parseLong(decodedNewToken.get(JWTClaimsEnum.EXP.getDisplayName()).getAsString()));
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime expirationDate = instant.atZone(zoneId).toLocalDateTime();

        UserToken newUserToken = UserToken.builder()
                .jti(decodedNewToken.get(JWTClaimsEnum.JTI.getDisplayName()).getAsString())
                .userId(decodedNewToken.get(JWTClaimsEnum.SUB.getDisplayName()).getAsString())
                .clientId(String.join(",", clientTypes.stream().map(ct->ct.getClientTypeName()).collect(Collectors.toSet())))
                .expirationDate(expirationDate)
                .refreshToken(new BCryptPasswordEncoder().encode(new String(Base64.getDecoder().decode(newRefreshToken))))
                .refreshTokenExp(LocalDateTime.now().plus(refreshTokenValidityDuration))
                .createdDate(LocalDateTime.now())
                .blocked(false)
                .build();

        userTokenRepository.save(newUserToken);

        return TokensDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private Claims verifyAccessToken(String accessToken, long clockSkew){
        Claims claims;
        try {
            claims = tokenUtils.getVerifiedTokenClaims(accessToken,clockSkew);
        }catch(Exception e) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID, e);
        }

        return claims;
    }

    private UserToken checkRefreshTokenState(String jti){
        UserToken userToken = userTokenRepository.findByJtiAndBlockedAndRefreshTokenExpAfter(
                jti, false, LocalDateTime.now());

        if (userToken == null) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        return userToken;
    }


}
