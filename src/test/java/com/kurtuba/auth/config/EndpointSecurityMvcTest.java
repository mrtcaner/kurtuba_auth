package com.kurtuba.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kurtuba.adm.controller.AdmUserController;
import com.kurtuba.adm.controller.AdmLoginController;
import com.kurtuba.adm.controller.AdmPageController;
import com.kurtuba.adm.controller.LocalizationController;
import com.kurtuba.adm.controller.LocalizationAdminPageController;
import com.kurtuba.adm.controller.OAuthClientAdminPageController;
import com.kurtuba.adm.controller.TokenAdminPageController;
import com.kurtuba.adm.controller.TokenManagementController;
import com.kurtuba.adm.controller.UserAdminPageController;
import com.kurtuba.adm.data.dto.AdmUserDto;
import com.kurtuba.adm.data.dto.UserAdminSearchCriteria;
import com.kurtuba.auth.controller.JwksController;
import com.kurtuba.auth.controller.LoginController;
import com.kurtuba.auth.controller.LoginPageController;
import com.kurtuba.auth.controller.RefreshTokenController;
import com.kurtuba.auth.controller.RegistrationController;
import com.kurtuba.auth.controller.SmsController;
import com.kurtuba.auth.controller.UserController;
import com.kurtuba.auth.data.dto.AvailableLocalizationOptionsDto;
import com.kurtuba.auth.data.dto.LoginCredentialsDto;
import com.kurtuba.auth.data.dto.PasswordResetRequestDto;
import com.kurtuba.auth.data.dto.RegistrationDto;
import com.kurtuba.auth.data.dto.TokenRefreshRequestDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.dto.UserBasicDto;
import com.kurtuba.auth.data.dto.UserDto;
import com.kurtuba.auth.data.dto.UserSettingDto;
import com.kurtuba.auth.data.dto.UserSettingLocaleDto;
import com.kurtuba.auth.data.enums.RateLimitPublicApi;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MessageJobStateType;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.mapper.UserMapper;
import com.kurtuba.auth.data.model.LocalizationMessage;
import com.kurtuba.auth.data.model.LocalizationSupportedCountry;
import com.kurtuba.auth.data.model.LocalizationSupportedLang;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.model.UserSetting;
import com.kurtuba.auth.data.model.UserToken;
import com.kurtuba.auth.data.dto.UserFcmTokenResponseDto;
import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.service.ISMSService;
import com.kurtuba.auth.service.LocalizationMessageService;
import com.kurtuba.auth.service.LoginService;
import com.kurtuba.auth.service.LogoutService;
import com.kurtuba.auth.service.MessageJobService;
import com.kurtuba.auth.service.RegistrationService;
import com.kurtuba.auth.service.UserRoleService;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.service.UserTokenService;
import com.kurtuba.auth.utils.TokenUtils;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        LoginController.class,
        LoginPageController.class,
        AdmLoginController.class,
        AdmPageController.class,
        LocalizationAdminPageController.class,
        TokenAdminPageController.class,
        UserAdminPageController.class,
        RefreshTokenController.class,
        RegistrationController.class,
        UserController.class,
        SmsController.class,
        JwksController.class,
        AdmUserController.class,
        TokenManagementController.class,
        LocalizationController.class,
        OAuthClientAdminPageController.class
})
@Import({DefaultSecurityConfig.class, EndpointSecurityMvcTest.TestRateLimitConfig.class})
@EnableWebSecurity
@ActiveProfiles({"test", "dev"})
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/auth/oauth2/jwks",
        "kurtuba.rate-limit.enabled=false"
})
class EndpointSecurityMvcTest {

    private static final String JWKS = "/auth/oauth2/jwks";
    private static final String LOGIN = "/auth/login";
    private static final String SERVICE_LOGIN = "/auth/service/login";
    private static final String ADM_LOGIN = "/auth/adm/login";
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

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private RegisteredClientRepository registeredClientRepository;

    @MockitoBean
    private LocalizationSupportedLangRepository localizationSupportedLangRepository;

    @MockitoBean
    private LocalizationSupportedCountryRepository localizationSupportedCountryRepository;

    @MockitoBean
    private UserTokenService userTokenService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private UserRoleService userRoleService;

    @MockitoBean
    private LogoutService logoutService;

    @MockitoBean
    private MessageJobService messageJobService;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private ISMSService smsService;

    @MockitoBean
    private LocalizationMessageService localizationMessageService;

    @MockitoBean
    private TokenUtils tokenUtils;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JWKSet jwkSet;

    @MockitoBean
    private com.kurtuba.auth.config.provider.CustomAuthenticationProvider customAuthenticationProvider;

    @BeforeEach
    void setUp() {
        when(jwkSet.toJSONObject(true)).thenReturn(Map.of("keys", List.of()));
        when(loginService.authenticateAndGetTokens(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(tokensResponse());
        when(loginService.authenticateAdminAndGetTokens(anyString(), anyString(), anyString(), any()))
                .thenReturn(tokensResponse());
        when(loginService.getTokensForUser(any(User.class), anyString(), anyString()))
                .thenReturn(tokensResponse());
        when(registeredClientRepository.findByClientId(anyString())).thenReturn(Optional.of(registeredClient()));
        when(registeredClientRepository.findById("client-1")).thenReturn(Optional.of(registeredClient()));
        when(registeredClientRepository.findByClientId("new-client")).thenReturn(Optional.empty());
        when(registeredClientRepository.findByClientId("service-client-id"))
                .thenReturn(Optional.of(serviceRegisteredClient()));
        when(registeredClientRepository.findByClientId("34ff7c95-ac55-4e7c-817e-6aa9333e21f6"))
                .thenReturn(Optional.of(adminWebRegisteredClient()));
        when(registeredClientRepository.findByClientName(anyString())).thenReturn(Optional.empty());
        when(registeredClientRepository.findByClientName("New Client")).thenReturn(Optional.empty());
        when(registeredClientRepository.findByClientType(RegisteredClientType.DEFAULT))
                .thenReturn(List.of(registeredClient()));
        when(registeredClientRepository.findAll()).thenReturn(List.of(registeredClient(), serviceRegisteredClient()));
        when(registeredClientRepository.save(any(RegisteredClient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localizationSupportedLangRepository.findAllByOrderByLanguageCodeAsc()).thenReturn(List.of(
                LocalizationSupportedLang.builder().id("lang-en").languageCode("en").createdDate(Instant.now()).build(),
                LocalizationSupportedLang.builder().id("lang-tr").languageCode("tr").createdDate(Instant.now()).build()
        ));
        when(localizationSupportedLangRepository.findByLanguageCode(anyString())).thenReturn(Optional.empty());
        when(localizationSupportedLangRepository.save(any(LocalizationSupportedLang.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(localizationSupportedCountryRepository.findAllByOrderByCountryCodeAsc()).thenReturn(List.of(
                LocalizationSupportedCountry.builder().id("country-tr").countryCode("tr").createdDate(Instant.now()).build(),
                LocalizationSupportedCountry.builder().id("country-us").countryCode("us").createdDate(Instant.now()).build()
        ));
        when(localizationSupportedCountryRepository.findByCountryCode(anyString())).thenReturn(Optional.empty());
        when(localizationSupportedCountryRepository.save(any(LocalizationSupportedCountry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0, String.class));
        when(userTokenService.refreshUserTokens(any())).thenReturn(tokensResponse());
        when(userTokenService.findAllByUserId(anyString())).thenReturn(List.of());
        when(userTokenService.findAllByUserIdAndBlocked(anyString(), anyBoolean())).thenReturn(List.of());
        when(userTokenService.checkDBIfTokenIsBlockedByJTI(anyString())).thenReturn(false);
        when(userTokenService.findByJTI(anyString())).thenReturn(Optional.of(userToken()));
        when(userService.requestResetPassword(anyString(), anyBoolean()))
                .thenReturn(UserMetaChange.builder().id("umc-reset").build());
        when(userService.requestChangeEmail(anyString(), anyString(), anyBoolean()))
                .thenReturn(UserMetaChange.builder().id("umc-email").build());
        when(userService.requestChangeMobile(anyString(), anyString()))
                .thenReturn(UserMetaChange.builder().id("umc-mobile").build());
        when(userService.getUserById(anyString())).thenReturn(Optional.of(user()));
        when(userService.getUserByEmail(anyString())).thenReturn(Optional.of(user()));
        when(userService.getUsersByIds(any())).thenReturn(List.of(user()));
        when(userService.searchUsers(any(UserAdminSearchCriteria.class))).thenReturn(List.of(user()));
        when(userService.searchAdmUsers(any(UserAdminSearchCriteria.class), any()))
                .thenReturn(new PageImpl<>(List.of(admUser())));
        when(userService.updateAdminSecurityAndActivity(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyInt()))
                .thenReturn(user());
        when(userMapper.mapToUserDto(any(User.class))).thenReturn(userDto());
        when(userMapper.mapToUserBasicDto(any(User.class))).thenReturn(userBasicDto());
        when(userRoleService.addRoleToUser(anyString(), anyString())).thenReturn(userRole());
        doNothing().when(userRoleService).removeRoleFromUser(anyString(), anyString());
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
                .thenReturn(UserMetaChange.builder().id("umc-register").userId("user-1").build());
        when(registrationService.getAvailableLocales()).thenReturn(AvailableLocalizationOptionsDto.builder()
                .languages(List.of("en", "tr"))
                .countries(List.of("tr", "us"))
                .build());
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
        when(localizationMessageService.search(anyString(), anyString(), anyString()))
                .thenReturn(List.of(localizationMessage()));
        when(localizationMessageService.create(any())).thenReturn(localizationMessage());
        when(localizationMessageService.update(any())).thenReturn(localizationMessage());
        when(localizationMessageService.finById(anyString())).thenReturn(Optional.of(localizationMessage()));
        doNothing().when(localizationMessageService).deleteById(anyString());
        when(messageJobService.findByUserMetaChangeIdAndUserId(anyString(), anyString()))
                .thenReturn(MessageJobStateType.PENDING);
        doNothing().when(userService).changePassword(any(), anyString());
        doNothing().when(userService).resetPasswordByLink(any());
        when(userService.verifyEmailByCode(anyString(), anyString())).thenReturn(null);
        when(userService.verifyEmailByLink(anyString()))
                .thenReturn(UserMetaChange.builder().id("umc-email-link").build());
        when(userService.verifyMobileByCode(anyString(), anyString())).thenReturn(null);
        doNothing().when(userService).updateUserPersonalInfo(anyString(), any());
        doNothing().when(userService).updateUsername(anyString(), anyString());
        doNothing().when(userService).updateUserLang(anyString(), anyString());
        doNothing().when(userService).upsertUserFcmToken(anyString(), anyString(), anyString(), anyString());
        doNothing().when(logoutService).doLogout(anyString());
        doNothing().when(logoutService).doLogoutFcm(anyString(), anyString());
        doNothing().when(userTokenService).changeTokenBlockByJTI(any(), anyBoolean());
        doNothing().when(userTokenService).blockUsersTokens(anyString());
    }

    private static RateLimitProperties.PublicApiProperties publicApiProperties(String pattern) {
        RateLimitProperties.PublicApiProperties properties = new RateLimitProperties.PublicApiProperties();
        properties.setPattern(pattern);
        properties.setCapacity(1);
        properties.setRefill(Duration.ofMinutes(1));
        return properties;
    }

    private AdmUserDto admUser() {
        User user = user();
        return AdmUserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .username(user.getUsername())
                .authProvider(user.getAuthProvider())
                .activated(user.isActivated())
                .locked(user.isLocked())
                .blocked(user.isBlocked())
                .showCaptcha(user.isShowCaptcha())
                .emailVerified(user.isEmailVerified())
                .mobileVerified(user.isMobileVerified())
                .createdDate(user.getCreatedDate())
                .build();
    }

    @TestConfiguration
    static class TestRateLimitConfig {
        @Bean
        RateLimitProperties rateLimitProperties() {
            RateLimitProperties properties = new RateLimitProperties();
            properties.getPublicApi().put(RateLimitPublicApi.REGISTRATION.getKey(), publicApiProperties("/auth/registration/**"));
            properties.getPublicApi().put(RateLimitPublicApi.LOGIN.getKey(), publicApiProperties("/auth/login"));
            properties.getPublicApi().put(RateLimitPublicApi.TOKEN_REFRESH.getKey(), publicApiProperties("/auth/token"));
            properties.getPublicApi().put(RateLimitPublicApi.WEB_TOKEN_REFRESH.getKey(), publicApiProperties("/auth/web/token"));
            properties.getPublicApi().put(RateLimitPublicApi.PASSWORD_RESET.getKey(), publicApiProperties("/auth/user/password/reset/**"));
            properties.getPublicApi().put(RateLimitPublicApi.SMS.getKey(), publicApiProperties("/auth/sms/**"));
            properties.getPublicApi().put(RateLimitPublicApi.VERIFICATION.getKey(), publicApiProperties("/auth/user/email/verification/link/**"));
            return properties;
        }
    }

    @Test
    void publicEndpointsRemainPublic() throws Exception {
        mockMvc.perform(get(LOGIN).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(get(ADM_LOGIN).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

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

        mockMvc.perform(post(ADM_LOGIN)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin@example.com")
                        .param("password", "a.123456"))
                .andExpect(status().is3xxRedirection());

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
    void adminLoginRemainsPublicEvenWithExpiredBearerSources() throws Exception {
        mockMvc.perform(get(ADM_LOGIN)
                        .header("Authorization", "Bearer expired-or-invalid-token")
                        .cookie(new jakarta.servlet.http.Cookie("jwt", "expired-or-invalid-token"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(post(ADM_LOGIN)
                        .header("Authorization", "Bearer expired-or-invalid-token")
                        .cookie(new jakarta.servlet.http.Cookie("jwt", "expired-or-invalid-token"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin@example.com")
                        .param("password", "a.123456"))
                .andExpect(status().is3xxRedirection());
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

        mockMvc.perform(delete(USER_BASE + "/email"))
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

        mockMvc.perform(delete(USER_BASE + "/email"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete(USER_BASE + "/mobile"))
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

        mockMvc.perform(put(USER_BASE + "/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice_test"
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
        String requestBody = objectMapper.writeValueAsString(Map.of("userIds", List.of("user-1")));

        mockMvc.perform(put(USER_BASE + "/info/users/basic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(jwtWithScope("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(USER_BASE + "/info/users/basic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(jwtWithScope("SERVICE")))
                .andExpect(status().isOk());
    }

    @Test
    void logoutClearsJwtCookie() throws Exception {
        mockMvc.perform(post(USER_BASE + "/logout")
                        .with(jwtWithScope("USER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("jwt=")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
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
        mockMvc.perform(get(ADM_BASE).accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADM_BASE).with(jwtWithScope("USER")).accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ADM_BASE + "/oauth-clients")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADM_BASE + "/oauth-clients")
                        .with(jwtWithScope("USER"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden());

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

        mockMvc.perform(delete(ADM_BASE + "/localization/loc-1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(ADM_BASE + "/localization/loc-1").with(jwtWithScope("USER")))
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

        mockMvc.perform(post(ADM_BASE + "/pages/tokens/block-user")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userId", "user-1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADM_BASE + "/pages/tokens/block-user")
                        .with(jwtWithScope("USER"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userId", "user-1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ADM_BASE + "/pages/tokens/block-jti")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userId", "user-1")
                        .param("jti", "test-jti"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADM_BASE + "/pages/tokens/block-jti")
                        .with(jwtWithScope("USER"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userId", "user-1")
                        .param("jti", "test-jti"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ADM_BASE + "/pages/users")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADM_BASE + "/pages/users")
                        .with(jwtWithScope("USER"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ADM_BASE + "/pages/users/user-1")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(ADM_BASE + "/pages/users/user-1")
                        .with(jwtWithScope("USER"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ADM_BASE + "/pages/users/user-1/roles")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("roleName", "ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADM_BASE + "/pages/users/user-1/roles")
                        .with(jwtWithScope("USER"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("roleName", "ADMIN"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ADM_BASE + "/pages/users/user-1/roles/remove")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("roleName", "ADMIN"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADM_BASE + "/pages/users/user-1/roles/remove")
                        .with(jwtWithScope("USER"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("roleName", "ADMIN"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ADM_BASE + "/pages/users/user-1/security")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("activated", "true")
                        .param("locked", "false")
                        .param("blocked", "false")
                        .param("showCaptcha", "false")
                        .param("failedLoginCount", "0"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(ADM_BASE + "/pages/users/user-1/security")
                        .with(jwtWithScope("USER"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("activated", "true")
                        .param("locked", "false")
                        .param("blocked", "false")
                        .param("showCaptcha", "false")
                        .param("failedLoginCount", "0"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allAdminRoutesAllowAdminScope() throws Exception {
        mockMvc.perform(get(ADM_BASE)
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(get(ADM_BASE + "/oauth-clients")
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(get(ADM_BASE + "/pages/localization")
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/auth/adm/pages/localization/messages"));

        mockMvc.perform(get(ADM_BASE + "/pages/localization/messages")
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(post(ADM_BASE + "/pages/localization/messages")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("languageCode", "en")
                        .param("key", "test.key")
                        .param("message", "value"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post(ADM_BASE + "/pages/localization/messages/loc-1")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "loc-1")
                        .param("message", "updated value"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post(ADM_BASE + "/pages/localization/messages/loc-1/delete")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("filterLang", "en")
                        .param("filterKey", "test")
                        .param("filterMessage", "value"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get(ADM_BASE + "/pages/localization/languages")
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(post(ADM_BASE + "/pages/localization/languages")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("languageCode", "ar"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post(ADM_BASE + "/pages/localization/languages/lang-en/delete")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get(ADM_BASE + "/pages/localization/countries")
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(post(ADM_BASE + "/pages/localization/countries")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("countryCode", "de"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post(ADM_BASE + "/pages/localization/countries/country-tr/delete")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get(ADM_BASE + "/pages/tokens")
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(post(ADM_BASE + "/pages/tokens/block-user")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userId", "user-1"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post(ADM_BASE + "/pages/tokens/block-jti")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userId", "user-1")
                        .param("jti", "test-jti"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get(ADM_BASE + "/pages/users")
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
        mockMvc.perform(get(ADM_BASE + "/pages/users/user-1")
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
        mockMvc.perform(post(ADM_BASE + "/pages/users/user-1/roles")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("roleName", "ADMIN"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post(ADM_BASE + "/pages/users/user-1/roles/remove")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("roleName", "ADMIN"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post(ADM_BASE + "/pages/users/user-1/security")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("activated", "true")
                        .param("locked", "false")
                        .param("blocked", "false")
                        .param("showCaptcha", "false")
                        .param("failedLoginCount", "0"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get(ADM_BASE + "/oauth-clients/client-1")
                        .with(jwtWithScope("ADMIN"))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());

        mockMvc.perform(post(ADM_BASE + "/oauth-clients")
                        .with(jwtWithScope("ADMIN"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("clientId", "new-client")
                        .param("clientName", "New Client")
                        .param("clientType", "WEB")
                        .param("accessTokenTtlMinutes", "60")
                        .param("refreshTokenEnabled", "true")
                        .param("refreshTokenTtlMinutes", "10080")
                        .param("sendTokenInCookie", "false")
                        .param("cookieMaxAgeSeconds", "0"))
                .andExpect(status().is3xxRedirection());

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

        mockMvc.perform(delete(ADM_BASE + "/localization/loc-1").with(jwtWithScope("ADMIN")))
                .andExpect(status().isNoContent());

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

    private RegisteredClient adminWebRegisteredClient() {
        return RegisteredClient.builder()
                .id("admin-web-client-1")
                .clientId("34ff7c95-ac55-4e7c-817e-6aa9333e21f6")
                .clientName("adm-web-client")
                .clientType(RegisteredClientType.WEB)
                .auds(Set.of("adm"))
                .scopes(Set.of("ADMIN", "USER"))
                .sendTokenInCookie(true)
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
        UserSetting userSetting = UserSetting.builder()
                .id("setting-1")
                .languageCode("en")
                .countryCode("TR")
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

    private UserDto userDto() {
        return UserDto.builder()
                .id("user-1")
                .name("Alice")
                .surname("Test")
                .username("alice_test")
                .email("user@example.com")
                .mobile("+905122345678")
                .authProvider(AuthProviderType.KURTUBA)
                .activated(true)
                .emailVerified(true)
                .mobileVerified(true)
                .createdDate(Instant.now())
                .userSetting(UserSettingDto.builder()
                        .id("setting-1")
                        .languageCode("en")
                        .countryCode("TR")
                        .locale(UserSettingLocaleDto.builder()
                                .id("TR_en")
                                .countryCode("TR")
                                .languageCode("en")
                                .createdDate(Instant.now())
                                .build())
                        .createdDate(Instant.now())
                        .build())
                .build();
    }

    private UserBasicDto userBasicDto() {
        return UserBasicDto.builder()
                .id("user-1")
                .name("Alice")
                .surname("Test")
                .username("alice_test")
                .email("user@example.com")
                .mobile("+905122345678")
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

    private UserToken userToken() {
        return UserToken.builder()
                .id("token-1")
                .userId("user-1")
                .refreshToken("refresh")
                .refreshTokenExp(Instant.now().plusSeconds(3600))
                .refreshTokenUsed(false)
                .jti("test-jti")
                .clientId("client-id")
                .auds(List.of("aud"))
                .scopes(List.of("ADMIN"))
                .blocked(false)
                .createdDate(Instant.now())
                .expirationDate(Instant.now().plusSeconds(3600))
                .build();
    }

    private com.kurtuba.auth.data.model.UserRole userRole() {
        return com.kurtuba.auth.data.model.UserRole.builder()
                .id("user-role-1")
                .user(user())
                .role(com.kurtuba.auth.data.model.Role.builder().id("role-1").name("ADMIN").build())
                .createdDate(Instant.now())
                .build();
    }
}
