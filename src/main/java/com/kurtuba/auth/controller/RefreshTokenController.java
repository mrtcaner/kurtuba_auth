package com.kurtuba.auth.controller;


import com.kurtuba.auth.data.dto.TokenRefreshRequestDto;
import com.kurtuba.auth.data.dto.TokenRefreshWebRequestDto;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserTokenService;
import com.kurtuba.auth.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/auth")
public class RefreshTokenController {

    final UserTokenService userTokenService;

    final TokenUtils tokenUtils;

    final RegisteredClientRepository registeredClientRepository;

    public RefreshTokenController(UserTokenService userTokenService, TokenUtils tokenUtils,
                                  RegisteredClientRepository registeredClientRepository) {
        this.userTokenService = userTokenService;
        this.tokenUtils = tokenUtils;
        this.registeredClientRepository = registeredClientRepository;
    }

    /**
     * Refresh token is rotated, can only be used once
     *
     * @param tokenRefreshRequestDto
     * @return
     */
    @PostMapping("/token")
    public ResponseEntity refreshTokens(@Valid @RequestBody TokenRefreshRequestDto tokenRefreshRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userTokenService.refreshUserTokens(tokenRefreshRequestDto));
    }

    /**
     * Web clients with jwt in cookies must call this endpoint
     *
     * @param tokenRefreshWebRequestDto
     * @param request
     * @return
     */
    @PostMapping("/web/token")
    public ResponseEntity refreshWebClientWithCookieTokens(@Valid @RequestBody TokenRefreshWebRequestDto tokenRefreshWebRequestDto,
                                                 HttpServletRequest request) {
        // find the jwt cookie
        String jwt = request.getCookies() == null ? null : Arrays
                .stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("jwt"))
                .map(cookie -> cookie.getValue())
                .findFirst()
                .orElse(null);
        if (jwt == null) {
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        RegisteredClient client = registeredClientRepository
                .findByClientId(tokenRefreshWebRequestDto.getClientId())
                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID));

        ResponseCookie cookie = ResponseCookie.from("jwt",
                        userTokenService.refreshWebClientWithCookieTokens(jwt, tokenRefreshWebRequestDto.getClientId(),
                                tokenRefreshWebRequestDto.getClientSecret()).accessToken)
                .httpOnly(client.isCookieHttpOnly())
                .secure(client.isCookieSecure())
                .path("/")
                .maxAge(client.getCookieMaxAgeSeconds())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("");
    }
}
