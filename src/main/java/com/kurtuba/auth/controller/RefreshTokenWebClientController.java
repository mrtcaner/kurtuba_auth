package com.kurtuba.auth.controller;


import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserTokenService;
import com.kurtuba.auth.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 *  web-clients receive their token in a httpOnly cookie. When their tokens expire they have to reach to this endpoint.
 *  Spring security when encountered a request with a bearer token, tries to validate it regardless of what was
 *  permitted by the security filters. This controller serves to an endpoint without any security restriction. There is
 *  a security filter specifically created for this controller's endpoint.
 */
@RestController
@RequestMapping("/public/auth")
public class RefreshTokenWebClientController {

    private static final int TOKEN_COOKIE_MAX_AGE_SECONDS = 7776000;

    final
    UserTokenService userTokenService;

    final
    TokenUtils tokenUtils;

    public RefreshTokenWebClientController(UserTokenService userTokenService, TokenUtils tokenUtils) {
        this.userTokenService = userTokenService;
        this.tokenUtils = tokenUtils;
    }

    @PostMapping("/web/token/refresh")
    public ResponseEntity refreshWebClientTokens(HttpServletRequest request, HttpServletResponse response) {
        // find the jwt cookie
        String jwt = request.getCookies() == null ? null : Arrays
                .stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("jwt"))
                .map(cookie -> cookie.getValue())
                .findFirst()
                .orElse(null);
        if(jwt == null){
            throw new BusinessLogicException(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID);
        }

        ResponseCookie cookie = ResponseCookie.from("jwt", userTokenService.refreshWebClientTokens(jwt))
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(TOKEN_COOKIE_MAX_AGE_SECONDS)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("");
    }
}
