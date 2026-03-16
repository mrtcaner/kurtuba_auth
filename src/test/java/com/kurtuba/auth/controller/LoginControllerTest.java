package com.kurtuba.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kurtuba.auth.data.dto.LoginCredentialsDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.handlers.BaseExceptionHandler;
import com.kurtuba.auth.error.handlers.GlobalExceptionHandler;
import com.kurtuba.auth.utils.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private com.kurtuba.auth.service.LoginService loginService;

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @Mock
    private TokenUtils tokenUtils;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new LoginController(loginService, registeredClientRepository, tokenUtils))
                .setControllerAdvice(new GlobalExceptionHandler(), new BaseExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void login_whenClientIdMissing_thenDefaultClientIsUsedAndTokensReturnedInBody() throws Exception {
        RegisteredClient defaultClient = RegisteredClient.builder()
                .id("client-1")
                .clientId("default-client")
                .clientSecret("default-secret")
                .clientType(RegisteredClientType.DEFAULT)
                .sendTokenInCookie(false)
                .auds(Set.of("mobile"))
                .build();
        LoginCredentialsDto request = LoginCredentialsDto.builder()
                .emailMobile("user@example.com")
                .password("password")
                .build();
        TokensResponseDto tokensResponseDto = TokensResponseDto.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(registeredClientRepository.findByClientType(RegisteredClientType.DEFAULT))
                .thenReturn(List.of(defaultClient));
        when(loginService.authenticateAndGetTokens(eq("user@example.com"), eq("password"),
                eq("default-client"), eq("default-secret"))).thenReturn(tokensResponseDto);
        when(registeredClientRepository.findByClientId("default-client")).thenReturn(Optional.of(defaultClient));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        verify(loginService).authenticateAndGetTokens("user@example.com", "password", "default-client", "default-secret");
    }

    @Test
    void login_whenClientSendsTokensInCookie_thenReturnsNoContentAndSetsJwtCookie() throws Exception {
        RegisteredClient webClient = RegisteredClient.builder()
                .id("client-2")
                .clientId("web-client")
                .clientSecret("web-secret")
                .clientType(RegisteredClientType.WEB)
                .sendTokenInCookie(true)
                .cookieMaxAgeSeconds(1800)
                .auds(Set.of("web"))
                .build();
        LoginCredentialsDto request = LoginCredentialsDto.builder()
                .emailMobile("user@example.com")
                .password("password")
                .clientId("web-client")
                .clientSecret("web-secret")
                .build();

        when(loginService.authenticateAndGetTokens("user@example.com", "password", "web-client", "web-secret"))
                .thenReturn(TokensResponseDto.builder().accessToken("jwt-token").refreshToken("refresh").build());
        when(registeredClientRepository.findByClientId("web-client")).thenReturn(Optional.of(webClient));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().httpOnly("jwt", true))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("jwt=jwt-token")));
    }
}
