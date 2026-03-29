package com.kurtuba.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kurtuba.adm.controller.AdmUserController;
import com.kurtuba.adm.controller.LocalizationController;
import com.kurtuba.adm.controller.TokenManagementController;
import com.kurtuba.auth.controller.JwksController;
import com.kurtuba.auth.controller.LoginController;
import com.kurtuba.auth.controller.RefreshTokenController;
import com.kurtuba.auth.controller.RegistrationController;
import com.kurtuba.auth.controller.SmsController;
import com.kurtuba.auth.controller.UserController;
import com.kurtuba.auth.data.dto.LoginCredentialsDto;
import com.kurtuba.auth.data.dto.PasswordResetRequestDto;
import com.kurtuba.auth.data.dto.RegistrationDto;
import com.kurtuba.auth.data.dto.TokenRefreshRequestDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MessageJobStateType;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.LocalizationAvailableLocale;
import com.kurtuba.auth.data.model.LocalizationMessage;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.model.UserSetting;
import com.kurtuba.auth.data.dto.UserFcmTokenResponseDto;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.service.ISMSService;
import com.kurtuba.auth.service.LocalizationMessageService;
import com.kurtuba.auth.service.LoginService;
import com.kurtuba.auth.service.LogoutService;
import com.kurtuba.auth.service.MessageJobService;
import com.kurtuba.auth.service.RegistrationService;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.service.UserTokenService;
import com.kurtuba.auth.utils.TokenUtils;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        LoginController.class,
        RefreshTokenController.class,
        RegistrationController.class,
        UserController.class,
        SmsController.class,
        JwksController.class,
        AdmUserController.class,
        TokenManagementController.class,
        LocalizationController.class
})
@Import(DefaultSecurityConfig.class)
@EnableWebSecurity
@ActiveProfiles({"test", "dev"})
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/auth/oauth2/jwks"
})
class EndpointSecurityMvcTest {

    private static final String JWKS = "/auth/oauth2/jwks";
    private static final String LOGIN = "/auth/login";
    private static final String SERVICE_LOGIN = "/auth/service/login";
    private static final String TOKEN_REFRESH = "/auth/token";
    private static final String WEB_TOKEN_REFRESH = "/auth/web/token";
    private static final String REGISTRATION_BASE = "/auth/registration";
    private static final String USER_BASE = "/auth/user";
    private static final String SMS_BASE = "/auth/sms";
    private static final String ADM_BASE = "/auth/adm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoginService loginService;

    @MockBean
    private RegisteredClientRepository registeredClientRepository;

    @MockBean
    private UserTokenService userTokenService;

    @MockBean
    private UserService userService;

    @MockBean
    private LogoutService logoutService;

    @MockBean
    private MessageJobService messageJobService;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private ISMSService smsService;

    @MockBean
    private LocalizationMessageService localizationMessageService;

    @MockBean
    private TokenUtils tokenUtils;

    @MockBean
    private JWKSet jwkSet;

    @MockBean
    private com.kurtuba.auth.config.provider.CustomAuthenticationProvider customAuthenticationProvider;

    @BeforeEach
    void setUp() {
        when(jwkSet.toJSONObject(true)).thenReturn(Map.of("keys", List.of()));
        when(loginService.authenticateAndGetTokens(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(tokensResponse());
        when(loginService.getTokensForUser(any(User.class), anyString(), anyString()))
                .thenReturn(tokensResponse());
        when(registeredClientRepository.findByClientId(anyString())).thenReturn(Optional.of(registeredClient()));
        when(registeredClientRepository.findByClientId("service-client-id"))
                .thenReturn(Optional.of(serviceRegisteredClient()));
        when(registeredClientRepository.findByClientType(RegisteredClientType.DEFAULT))
                .thenReturn(List.of(registeredClient()));
        when(userTokenService.refreshUserTokens(any())).thenReturn(tokensResponse());
        when(userTokenService.findAllByUserId(anyString())).thenReturn(List.of());
        when(userTokenService.findAllByUserIdAndBlocked(anyString(), anyBoolean())).thenReturn(List.of());
        when(userTokenService.checkDBIfTokenIsBlockedByJTI(anyString())).thenReturn(false);
        when(userService.requestResetPassword(anyString(), anyBoolean()))
                .thenReturn(UserMetaChange.builder().id("umc-reset").build());
        when(userService.requestChangeEmail(anyString(), anyString(), anyBoolean()))
                .thenReturn(UserMetaChange.builder().id("umc-email").build());
        when(userService.requestChangeMobile(anyString(), anyString()))
                .thenReturn(UserMetaChange.builder().id("umc-mobile").build());
        when(userService.getUserById(anyString())).thenReturn(Optional.of(user()));
        when(userService.getUserByEmail(anyString())).thenReturn(Optional.of(user()));
        when(userService.resetPasswordByCode(any())).thenReturn(tokensResponse());
        when(userService.validatePasswordResetLinkParam(anyString()))
                .thenReturn(UserMetaChange.builder().id("umc-reset-link").build());
        when(userService.getUserFcmTokens(anyString()))
                .thenReturn(List.of(UserFcmTokenResponseDto.builder()
                        .fcmToken("fcm-token")
                        .clientId("client-id")
                        .clientType("WEB")
                        .userId("user-1")
                        .updatedAt(Instant.now())
                        .build()));
        when(registrationService.register(any()))
                .thenReturn(UserMetaChange.builder().id("umc-register").build());
        when(registrationService.getAvailableLocales()).thenReturn(List.of(
                LocalizationAvailableLocale.builder().languageCode("en").countryCode("us").build(),
                LocalizationAvailableLocale.builder().languageCode("tr").countryCode("tr").build()
        ));
        when(registrationService.registerByAnotherProvider(any())).thenReturn(user());
        when(registrationService.sendAccountActivationMessage(anyString(), anyBoolean()))
                .thenReturn(UserMetaChange.builder().id("umc-activation").build());
        when(registrationService.activateAccountByCode(anyString(), anyString(), any(), any()))
                .thenReturn(tokensResponse());
        when(registrationService.activateAccountByLink(anyString()))
                .thenReturn(UserMetaChange.builder()
                        .id("umc-activation-link")
                        .contactType(ContactType.EMAIL)
                        .build());
        when(userTokenService.refreshWebClientWithCookieTokens(anyString(), anyString(), anyString()))
                .thenReturn(tokensResponse());
        when(smsService.sendSMS(anyString(), anyString(), anyString())).thenReturn("queued");
        when(smsService.sendVerificationSMS(anyString())).thenReturn(null);
        when(smsService.checkVerification(anyString(), anyString())).thenReturn(true);
        when(smsService.deleteVerification(anyString())).thenReturn("deleted");
        when(localizationMessageService.findAll()).thenReturn(List.of());
        when(localizationMessageService.findByLanguageCodeAndMessageKey(anyString(), anyString()))
                .thenReturn(localizationMessage());
        when(localizationMessageService.create(any())).thenReturn(localizationMessage());
        when(localizationMessageService.update(any())).thenReturn(localizationMessage());
        when(localizationMessageService.finById(anyString())).thenReturn(Optional.of(localizationMessage()));
        when(messageJobService.findByUserMetaChangeIdAndUserId(anyString(), anyString()))
                .thenReturn(MessageJobStateType.PENDING);
        doNothing().when(userService).changePassword(any(), anyString());
        doNothing().when(userService).resetPasswordByLink(any());
        when(userService.verifyEmailByCode(anyString(), anyString())).thenReturn(null);
        when(userService.verifyEmailByLink(anyString()))
                .thenReturn(UserMetaChange.builder().id("umc-email-link").build());
        when(userService.verifyMobileByCode(anyString(), anyString())).thenReturn(null);
        doNothing().when(userService).updateUserPersonalInfo(anyString(), any());
        doNothing().when(userService).updateUserLang(anyString(), anyString());
        doNothing().when(userService).upsertUserFcmToken(anyString(), anyString(), anyString(), anyString());
        doNothing().when(logoutService).doLogout(anyString());
        doNothing().when(logoutService).doLogoutFcm(anyString(), anyString());
        doNothing().when(userTokenService).changeTokenBlockByJTI(any(), anyBoolean());
        doNothing().when(userTokenService).blockUsersTokens(anyString());
    }

    @Test
    void publicEndpointsRemainPublic() throws Exception {
        mockMvc.perform(get(JWKS))
                .andExpect(status().isOk());

        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginCredentialsDto.builder()
                                .emailMobile("user@example.com")
                                .password("a.123456")
                                .clientId("client-id")
                                .clientSecret("client-secret")
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(post(TOKEN_REFRESH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TokenRefreshRequestDto.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .clientId("client-id")
                                .clientSecret("client-secret")
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REGISTRATION_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto())))
                .andExpect(status().isCreated());

        mockMvc.perform(get(REGISTRATION_BASE + "/locales"))
                .andExpect(status().isOk());

        mockMvc.perform(post(USER_BASE + "/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PasswordResetRequestDto.builder()
                                .emailMobile("user@example.com")
                                .byCode(false)
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(get(USER_BASE + "/email/verification/link/test-link"))
                .andExpect(status().isOk());

        mockMvc.perform(post(SMS_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "+905001112233",
                                  "sender": "Kurtuba",
                                  "message": "test"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void tokenRefreshEndpointsRemainPublicEvenWithBearerTokenHeader() throws Exception {
        mockMvc.perform(post(TOKEN_REFRESH)
                        .header("Authorization", "Bearer expired-or-invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TokenRefreshRequestDto.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .clientId("client-id")
                                .clientSecret("client-secret")
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(post(WEB_TOKEN_REFRESH)
                        .header("Authorization", "Bearer expired-or-invalid-token")
                        .cookie(new jakarta.servlet.http.Cookie("jwt", "access-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "client-id",
                                  "clientSecret": "client-secret"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void allPublicRoutesRemainAnonymousAccessible() throws Exception {
        mockMvc.perform(post(SERVICE_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "service-client-id",
                                  "clientSecret": "service-secret"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post(WEB_TOKEN_REFRESH)
                        .cookie(new jakarta.servlet.http.Cookie("jwt", "access-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "client-id",
                                  "clientSecret": "client-secret"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post(REGISTRATION_BASE + "/other-provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GOOGLE",
                                  "providerClientId": "client-id",
                                  "token": "provider-token",
                                  "languageCode": "en",
                                  "countryCode": "TR"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get(REGISTRATION_BASE + "/username/available/alice"))
                .andExpect(status().isOk());

        mockMvc.perform(get(REGISTRATION_BASE + "/email/available/user@example.com"))
                .andExpect(status().isOk());

        mockMvc.perform(get(REGISTRATION_BASE + "/mobile/available/+905122345678"))
                .andExpect(status().isOk());

        mockMvc.perform(post(REGISTRATION_BASE + "/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailMobile": "user@example.com",
                                  "byCode": true
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put(REGISTRATION_BASE + "/activation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailMobile": "user@example.com",
                                  "code": "123456",
                                  "clientId": "client-id",
                                  "clientSecret": "client-secret"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get(REGISTRATION_BASE + "/activation/link/test-link"))
                .andExpect(status().isOk());

        mockMvc.perform(put(USER_BASE + "/password/reset/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailMobile": "user@example.com",
                                  "code": "123456",
                                  "newPassword": "a.123456",
                                  "repeatNewPassword": "a.123456",
                                  "clientId": "client-id",
                                  "clientSecret": "client-secret"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get(USER_BASE + "/password/reset/password-reset/test-link"))
                .andExpect(status().isOk());

        mockMvc.perform(post(USER_BASE + "/password/reset/password-reset")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("linkParam", "test-link")
                        .param("newPassword", "a.123456")
                        .param("repeatNewPassword", "a.123456"))
                .andExpect(status().isOk());

        mockMvc.perform(get(USER_BASE + "/password/reset/forgot-password"))
                .andExpect(status().isOk());

        mockMvc.perform(post(USER_BASE + "/password/reset/forgot-password")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("emailMobile", "user@example.com"))
                .andExpect(status().isOk());

        mockMvc.perform(post(SMS_BASE + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "+905001112233"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put(SMS_BASE + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "+905001112233",
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete(SMS_BASE + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sid": "sid-1"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post(SMS_BASE + "/message-status"))
                .andExpect(status().isOk());
    }

    @Test
    void securedUserEndpointsRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get(USER_BASE + "/info").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put(USER_BASE + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "a.123456",
                                  "newPassword": "b.123456",
                                  "repeatNewPassword": "b.123456"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(USER_BASE + "/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "byCode": true
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allAuthenticatedUserRoutesRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get(USER_BASE + "/locale"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(USER_BASE + "/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "byCode": true
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put(USER_BASE + "/email/verification/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(USER_BASE + "/mobile/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mobile": "+905122345678"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put(USER_BASE + "/mobile/verification/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put(USER_BASE + "/personal-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice",
                                  "surname": "Test"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put(USER_BASE + "/lang")
                        .param("langCode", "en"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(USER_BASE + "/verification/send-status")
                        .param("userMetaChangeId", "umc-1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(USER_BASE + "/fcm-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fcmToken": "fcm-token",
                                  "firebaseInstallationId": "firebase-installation-id"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(USER_BASE + "/fcm-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(USER_BASE + "/logout"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(USER_BASE + "/logout/firebase")
                        .param("firebaseInstallationId", "firebase-installation-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void serviceOnlyEndpointStillRequiresServiceScope() throws Exception {
        mockMvc.perform(get(USER_BASE + "/user-1")
                        .with(jwtWithScope("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(USER_BASE + "/user-1")
                        .with(jwtWithScope("SERVICE")))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointsStillRequireAdminScope() throws Exception {
        mockMvc.perform(get(ADM_BASE + "/token/blocked/jti/test-jti")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(ADM_BASE + "/token/blocked/jti/test-jti")
                        .with(jwtWithScope("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ADM_BASE + "/token/blocked/jti/test-jti")
                        .with(jwtWithScope("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void allAdminRoutesRejectAnonymousAndNonAdminRequests() throws Exception {
        mockMvc.perform(get(ADM_BASE + "/localization"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADM_BASE + "/localization").with(jwtWithScope("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ADM_BASE + "/localization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "en",
                                  "key": "test.key",
                                  "message": "value"
                                }
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADM_BASE + "/localization")
                        .with(jwtWithScope("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "en",
                                  "key": "test.key",
                                  "message": "value"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(ADM_BASE + "/localization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "loc-1",
                                  "message": "value"
                                }
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(ADM_BASE + "/localization")
                        .with(jwtWithScope("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "loc-1",
                                  "message": "value"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(ADM_BASE + "/localization/cache"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(ADM_BASE + "/localization/cache").with(jwtWithScope("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ADM_BASE + "/user/token/user-1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADM_BASE + "/user/token/user-1").with(jwtWithScope("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ADM_BASE + "/token/user-1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADM_BASE + "/token/user-1").with(jwtWithScope("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(ADM_BASE + "/token/blocked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tokenIds": ["token-1"]
                                }
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(ADM_BASE + "/token/blocked")
                        .with(jwtWithScope("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tokenIds": ["token-1"]
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(ADM_BASE + "/token/blocked/user/user-1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(ADM_BASE + "/token/blocked/user/user-1").with(jwtWithScope("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ADM_BASE + "/token/blocked/user/user-1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADM_BASE + "/token/blocked/user/user-1").with(jwtWithScope("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allAdminRoutesAllowAdminScope() throws Exception {
        mockMvc.perform(get(ADM_BASE + "/localization").with(jwtWithScope("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(post(ADM_BASE + "/localization")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "en",
                                  "key": "test.key",
                                  "message": "value"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put(ADM_BASE + "/localization")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": "loc-1",
                                  "message": "value"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete(ADM_BASE + "/localization/cache").with(jwtWithScope("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get(ADM_BASE + "/user/token/user-1").with(jwtWithScope("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get(ADM_BASE + "/token/user-1").with(jwtWithScope("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(put(ADM_BASE + "/token/blocked")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tokenIds": ["token-1"]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put(ADM_BASE + "/token/blocked/user/user-1").with(jwtWithScope("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get(ADM_BASE + "/token/blocked/user/user-1").with(jwtWithScope("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get(ADM_BASE + "/token/blocked/jti/test-jti").with(jwtWithScope("ADMIN")))
                .andExpect(status().isOk());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithScope(String scope) {
        return jwt()
                .jwt(jwt -> jwt.subject("principal-" + scope.toLowerCase()))
                .authorities(new SimpleGrantedAuthority("SCOPE_" + scope));
    }

    private TokensResponseDto tokensResponse() {
        return TokensResponseDto.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
    }

    private RegisteredClient registeredClient() {
        return RegisteredClient.builder()
                .id("client-1")
                .clientId("client-id")
                .clientName("default-client")
                .clientSecret("client-secret")
                .clientType(RegisteredClientType.DEFAULT)
                .auds(Set.of("aud"))
                .scopes(Set.of("USER"))
                .sendTokenInCookie(false)
                .cookieMaxAgeSeconds(3600)
                .build();
    }

    private RegisteredClient serviceRegisteredClient() {
        return RegisteredClient.builder()
                .id("service-client-1")
                .clientId("service-client-id")
                .clientName("service-client")
                .clientSecret(new BCryptPasswordEncoder().encode("service-secret"))
                .clientType(RegisteredClientType.SERVICE)
                .auds(Set.of("aud"))
                .scopes(Set.of("SERVICE"))
                .sendTokenInCookie(false)
                .cookieMaxAgeSeconds(3600)
                .build();
    }

    private RegistrationDto registrationDto() {
        return RegistrationDto.builder()
                .name("Alice")
                .surname("Test")
                .username("alice_test")
                .email("user@example.com")
                .mobile("+905122345678")
                .password("a.123456")
                .authProvider(AuthProviderType.KURTUBA)
                .preferredVerificationContact(ContactType.EMAIL)
                .verificationByCode(true)
                .languageCode("en")
                .countryCode("TR")
                .build();
    }

    private User user() {
        LocalizationAvailableLocale locale = LocalizationAvailableLocale.builder()
                .id("locale-1")
                .languageCode("en")
                .countryCode("TR")
                .createdDate(Instant.now())
                .build();

        UserSetting userSetting = UserSetting.builder()
                .id("setting-1")
                .locale(locale)
                .createdDate(Instant.now())
                .build();

        return User.builder()
                .id("user-1")
                .name("Alice")
                .surname("Test")
                .username("alice_test")
                .email("user@example.com")
                .mobile("+905122345678")
                .password("a.123456")
                .authProvider(AuthProviderType.KURTUBA)
                .userSetting(userSetting)
                .activated(true)
                .emailVerified(true)
                .mobileVerified(true)
                .createdDate(Instant.now())
                .build();
    }

    private LocalizationMessage localizationMessage() {
        return LocalizationMessage.builder()
                .id("loc-1")
                .languageCode("en")
                .messageKey("test.key")
                .message("value")
                .createdDate(Instant.now())
                .build();
    }
}
