package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.TokenRefreshWebRequestDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserTokenService;
import com.kurtuba.auth.utils.TokenUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenControllerTest {

    @Mock
    private UserTokenService userTokenService;

    @Mock
    private TokenUtils tokenUtils;

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    private RefreshTokenController refreshTokenController;

    @BeforeEach
    void setUp() {
        refreshTokenController = new RefreshTokenController(userTokenService, tokenUtils, registeredClientRepository);
    }

    @Test
    void refreshWebClientWithCookieTokens_whenJwtCookieIsMissing_thenThrowsInvalidRefreshToken() {
        TokenRefreshWebRequestDto request = TokenRefreshWebRequestDto.builder()
                .clientId("web-client")
                .clientSecret("secret")
                .build();

        BusinessLogicException exception = assertThrows(BusinessLogicException.class,
                () -> refreshTokenController.refreshWebClientWithCookieTokens(request, new MockHttpServletRequest()));

        assertEquals(ErrorEnum.AUTH_REFRESH_TOKEN_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void refreshWebClientWithCookieTokens_whenJwtCookieExists_thenRotatesCookie() {
        TokenRefreshWebRequestDto request = TokenRefreshWebRequestDto.builder()
                .clientId("web-client")
                .clientSecret("secret")
                .build();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setCookies(new Cookie("jwt", "old-jwt"));

        when(userTokenService.refreshWebClientWithCookieTokens("old-jwt", "web-client", "secret"))
                .thenReturn(TokensResponseDto.builder().accessToken("new-jwt").build());
        when(registeredClientRepository.findByClientId("web-client"))
                .thenReturn(Optional.of(RegisteredClient.builder()
                        .clientId("web-client")
                        .cookieMaxAgeSeconds(3600)
                        .build()));

        ResponseEntity<?> response = refreshTokenController.refreshWebClientWithCookieTokens(request, servletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("", response.getBody());
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertTrue(setCookie.contains("jwt=new-jwt"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Max-Age=3600"));
    }
}
