package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.ClientType;
import com.kurtuba.auth.data.model.JWTClaimsEnum;
import com.kurtuba.auth.data.model.UserToken;
import com.kurtuba.auth.data.model.dto.TokensDto;
import com.kurtuba.auth.data.model.dto.UserDto;
import com.kurtuba.auth.data.repository.UserTokenRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.TokenUtils;
import com.nimbusds.jose.shaded.gson.JsonObject;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Service
public class UserTokenService {

    final
    UserTokenRepository userTokenRepository;

    final
    TokenUtils tokenUtils;

    final
    UserService userService;

    public UserTokenService(UserTokenRepository userTokenRepository, TokenUtils tokenUtils, UserService userService) {
        this.userTokenRepository = userTokenRepository;
        this.tokenUtils = tokenUtils;
        this.userService = userService;
    }

    @Transactional
    public void save(UserToken userToken){
        userTokenRepository.save(userToken);
    }

    @Transactional
    public TokensDto refreshUserTokens(TokensDto tokenDto){
        Claims claims = verifyTokens(tokenDto);
        UserDto userDto = userService.getUserById(claims.getSubject());
        if(userDto.isActivated() && userDto.isEmailValidated() && !userDto.isLocked() && !userDto.isShowCaptcha()){
            return createAndSaveTokens(claims.getSubject(),ClientType.fromName(claims.getAudience().toString()));
        }
        throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
    }

    /**
     * TODO
     * This method must not be accessed without user credentials or a valid refresh token.
     * That means token-validation/user-authentication must be carried out inside this method.
     * @param userId
     * @param clientType
     * @return
     */
    @Transactional
    public TokensDto createAndSaveTokens(String userId, ClientType clientType){
        String newAccessToken = tokenUtils.generateToken(userId, clientType);
        String newRefreshToken = tokenUtils.generateRefreshToken();

        JsonObject decodedNewToken = TokenUtils.decodeTokenPayload(newAccessToken);

        Instant instant = Instant.ofEpochSecond(Long.parseLong(decodedNewToken.get(JWTClaimsEnum.EXP.getDisplayName()).getAsString()));
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime expirationDate = instant.atZone(zoneId).toLocalDateTime();

        UserToken newUserToken = UserToken.builder()
                .jti(decodedNewToken.get(JWTClaimsEnum.JTI.getDisplayName()).getAsString())
                .userId(decodedNewToken.get(JWTClaimsEnum.SUB.getDisplayName()).getAsString())
                .clientId(clientType.getClientTypeName())
                .expirationDate(expirationDate)
                .refreshToken(new BCryptPasswordEncoder().encode(new String(Base64.getDecoder().decode(newRefreshToken))))
                .refreshTokenExp(LocalDateTime.now().plusMonths(3))// todo put the values to properties
                .createdDate(LocalDateTime.now())
                .blocked(false)
                .build();

        userTokenRepository.save(newUserToken);

        return TokensDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }


    public Claims verifyTokens(TokensDto tokensDto){
        Claims claims;
        try {
            claims = tokenUtils.getVerifiedTokenClaims(tokensDto.getAccessToken());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        UserToken userToken = userTokenRepository.findByJtiAndBlockedAndRefreshTokenExpAfter(
                claims.get("jti").toString(), false, LocalDateTime.now());

        if (userToken == null) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        //Decode refresh token
        byte[] decodedBytes = Base64.getDecoder().decode(tokensDto.getRefreshToken());
        String decodedRefreshToken = new String(decodedBytes);

        //Compare saved bcrypt hash with received token
        if (!new BCryptPasswordEncoder().matches(decodedRefreshToken, userToken.getRefreshToken())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        return claims;
    }


}
