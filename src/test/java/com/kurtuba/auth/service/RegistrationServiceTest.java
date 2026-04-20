package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.RegistrationDto;
import com.kurtuba.auth.data.dto.RegistrationOtherProviderDto;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.mapper.UserMapper;
import com.kurtuba.auth.data.model.*;
import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock
    private UserMetaChangeService userMetaChangeService;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private LocalizationSupportedCountryRepository localizationSupportedCountryRepository;

    @Mock
    private LocalizationSupportedLangRepository localizationSupportedLangRepository;

    @Mock
    private MessageJobService messageJobService;

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private RegistrationService registrationService;


    RegistrationDto registrationDto;

    UserMetaChange emailActivationCodeUserMetaChange;

    UserMetaChange emailActivationLinkUserMetaChange;

    String userMetaChangeByCodeId = "byCode";
    String userMetaChangeByLinkId = "byLink";

    private User buildMappedUser(RegistrationDto source) {
        UserSetting userSetting = TestUtils.defaultUserSettingBuilder();
        userSetting.setLanguageCode(source.getLanguageCode());
        userSetting.setCountryCode(source.getCountryCode());

        User user = User.builder()
                .name(source.getName())
                .surname(source.getSurname())
                .username(StringUtils.hasLength(source.getUsername()) ? source.getUsername() : null)
                .email(StringUtils.hasLength(source.getEmail()) ? source.getEmail() : null)
                .mobile(StringUtils.hasLength(source.getMobile()) ? source.getMobile() : null)
                .password(source.getPassword())
                .authProvider(source.getAuthProvider())
                .userSetting(userSetting)
                .activated(false)
                .locked(false)
                .failedLoginCount(0)
                .showCaptcha(false)
                .emailVerified(false)
                .mobileVerified(false)
                .createdDate(Instant.now())
                .lastLoginAttempt(Instant.now())
                .build();
        userSetting.setUser(user);
        return user;
    }

    @Nested
    class FirstNestedClass {


        @BeforeEach
        public void setup() {

            registrationDto = RegistrationDto.builder()
                    .name("aa")
                    .surname("bb")
                    .username("cc")
                    .email("user@user.com")
                    .mobile("+905122345678")
                    .password("a.123456")
                    .authProvider(AuthProviderType.KURTUBA)
                    .preferredVerificationContact(ContactType.EMAIL)
                    .verificationByCode(true)
                    .languageCode("tr")
                    .countryCode("tr")
                    .build();

            UserSetting userSetting = TestUtils.defaultUserSettingBuilder();
            userSetting.setLanguageCode(registrationDto.getLanguageCode());
            userSetting.setCountryCode(registrationDto.getCountryCode());

            UserRole userRole = TestUtils.defaultUserRoleBuilder();

            User savedUser = User.builder()
                    .id("1")
                    .name(registrationDto.getName())
                    .surname(registrationDto.getSurname())
                    .email(registrationDto.getEmail())
                    .emailVerified(false)
                    .mobile(registrationDto.getMobile())
                    .mobileVerified(false)
                    .username(registrationDto.getUsername())
                    .password(new BCryptPasswordEncoder().encode(registrationDto.getPassword()))
                    .authProvider(registrationDto.getAuthProvider())
                    .activated(false)
                    .locked(false)
                    .failedLoginCount(0)
                    .showCaptcha(false)
                    .userSetting(userSetting)
                    .userRoles(List.of(userRole))
                    .birthdate(null)
                    .createdDate(Instant.now())
                    .build();
            userRole.setUser(savedUser);
            userSetting.setUser(savedUser);

            emailActivationCodeUserMetaChange = UserMetaChange.builder()
                                                              .id(userMetaChangeByCodeId)
                                                              .meta(registrationDto.getEmail())
                                                              .metaOperationType(MetaOperationType.ACCOUNT_ACTIVATION)
                                                              .contactType(ContactType.EMAIL)
                                                              .userId(savedUser.getId())
                                                              .maxTryCount(3)
                                                              .tryCount(0)
                                                              .code("123456")
                                                              .executed(false)
                                                              .expirationDate(Instant.now().plus(Duration.ofHours(24))) // todo: get expiration time properly
                                                              .createdDate(Instant.now())
                                                              .build();

            emailActivationLinkUserMetaChange = UserMetaChange.builder()
                    .id(userMetaChangeByLinkId)
                    .meta(registrationDto.getEmail())
                    .metaOperationType(MetaOperationType.ACCOUNT_ACTIVATION)
                    .contactType(ContactType.EMAIL)
                    .userId(savedUser.getId())
                    .linkParam("link")
                    .executed(false)
                    .expirationDate(Instant.now().plus(Duration.ofHours(24))) // todo: get expiration time properly
                    .createdDate(Instant.now())
                    .build();

            when(userService.saveUser(any(User.class))).then(invocationOnMock -> {
                User usr = invocationOnMock.getArgument(0);
                assertEquals(usr.getName(), savedUser.getName());
                assertEquals(usr.getSurname(), savedUser.getSurname());
                assertEquals(usr.getUsername(), savedUser.getUsername());
                assertEquals(usr.getEmail(), savedUser.getEmail());
                assertEquals(usr.isEmailVerified(), (savedUser.isEmailVerified()));
                assertEquals(usr.getMobile(), savedUser.getMobile());
                assertEquals(usr.isMobileVerified(), (savedUser.isMobileVerified()));
                assertFalse(new BCryptPasswordEncoder().matches(usr.getPassword(), savedUser.getPassword()));
                assertEquals(usr.getAuthProvider(), savedUser.getAuthProvider());
                assertEquals(usr.isActivated(), (savedUser.isActivated()));
                assertEquals(usr.isLocked(), (savedUser.isLocked()));
                assertEquals(usr.getFailedLoginCount(), (savedUser.getFailedLoginCount()));
                assertEquals(usr.isShowCaptcha(), (savedUser.isShowCaptcha()));
                assertSame(usr.getBirthdate(), (savedUser.getBirthdate()));
                assertNotNull(usr.getCreatedDate());
                //todo: check also

                return savedUser;
            });

            when(userService.getUserByEmail(registrationDto.getEmail()))
                    .thenReturn(Optional.empty(), Optional.of(savedUser));
            when(userService.getUserByMobile(registrationDto.getMobile()))
                    .thenReturn(Optional.empty());
            when(userMapper.maptoUser(any(RegistrationDto.class)))
                    .then(invocationOnMock -> buildMappedUser(invocationOnMock.getArgument(0)));

            when(userMetaChangeService.create((any(UserMetaChange.class)))).then(invocationOnMock -> {
                UserMetaChange usrMtChange = invocationOnMock.getArgument(0);
                if (StringUtils.hasLength(usrMtChange.getCode())) {
                    return emailActivationCodeUserMetaChange;
                } else {
                    return emailActivationLinkUserMetaChange;
                }
            });

            when(localizationSupportedLangRepository.findByLanguageCode(anyString()))
                    .then(invocationOnMock -> Optional.of(LocalizationSupportedLang.builder()
                            .languageCode(invocationOnMock.getArgument(0))
                            .createdDate(Instant.now())
                            .build()));
            when(localizationSupportedCountryRepository.findByCountryCode(anyString()))
                    .then(invocationOnMock -> Optional.of(LocalizationSupportedCountry.builder()
                            .countryCode(invocationOnMock.getArgument(0))
                            .createdDate(Instant.now())
                            .build()));

            when(userRoleService.create(any(UserRole.class))).then(invocationOnMock -> {
                UserRole usrRole = invocationOnMock.getArgument(0);
                usrRole.setId("userRoleId");
                usrRole.setCreatedDate(Instant.now());
                return usrRole;
            });
        }

        @Test
        public void createUser_whenGivenValidRegistrationDto_thenReturnSavedUser() {
            doNothing().when(messageJobService).sendAccountActivationCodeMail(anyString(), anyString(), anyString(),
                    nullable(String.class));
            //default registrationDto is set to account activation by email using code
            String metaChangeId = registrationService.register(registrationDto).getId();
            assertEquals(metaChangeId, emailActivationCodeUserMetaChange.getId());
        }

        @Test
        public void createUser_whenGivenValidRegistrationDtoWithVerifyByLink_thenReturnSavedUser() {
            doNothing().when(messageJobService).sendAccountActivationLinkMail(anyString(), anyString(), anyString(),
                    nullable(String.class));
            //default registrationDto is set to account activation by email using code
            registrationDto.setVerificationByCode(false);
            String metaChangeId = registrationService.register(registrationDto).getId();
            assertEquals(metaChangeId, emailActivationLinkUserMetaChange.getId());
        }

    }

    @Nested
    class SecondNestedClass {

        @BeforeEach
        public void setup() {

            registrationDto = RegistrationDto.builder()
                    .name("aa")
                    .surname("bb")
                    .username("cc")
                    .email("user@user.com")
                    .mobile("+905122345678")
                    .password("a.123456")
                    .authProvider(AuthProviderType.KURTUBA)
                    .preferredVerificationContact(ContactType.EMAIL)
                    .verificationByCode(true)
                    .languageCode("tr")
                    .countryCode("tr")
                    .build();
        }

        @Test
        public void createUser_whenGivenMissingEmailAndMobile_thenThrowException() {
            registrationDto.setEmail("");
            registrationDto.setMobile("");
            BusinessLogicException exception = assertThrows(BusinessLogicException.class,
                    () -> registrationService.register(registrationDto));
            assertEquals(exception.getErrorCode(), (int) ErrorEnum.USER_CONTACT_REQUIRED.getCode());

        }
    }

    @Nested
    class RegisterByAnotherProviderTests {

        RegistrationOtherProviderDto providerDto;
        RegistrationDto decodedRegistration;
        User existingUser;
        RegistrationService spyRegistrationService;

        @BeforeEach
        void setup() {
            providerDto = RegistrationOtherProviderDto.builder()
                    .provider(AuthProviderType.GOOGLE)
                    .providerClientId("google-client")
                    .token("token")
                    .redirectUri("https://kurtuba.app/deeplink")
                    .languageCode("en")
                    .countryCode("tr")
                    .build();

            decodedRegistration = RegistrationDto.builder()
                    .name("Google")
                    .surname("User")
                    .email("user@user.com")
                    .password("a.123456")
                    .authProvider(AuthProviderType.GOOGLE)
                    .languageCode("en")
                    .countryCode("tr")
                    .preferredVerificationContact(ContactType.EMAIL)
                    .verificationByCode(true)
                    .build();

            UserSetting userSetting = TestUtils.defaultUserSettingBuilder();
            UserRole userRole = TestUtils.defaultUserRoleBuilder();

            existingUser = User.builder()
                    .id("existing-user")
                    .name("Manual")
                    .surname("User")
                    .email(decodedRegistration.getEmail())
                    .password(new BCryptPasswordEncoder().encode("manual-pass"))
                    .authProvider(AuthProviderType.KURTUBA)
                    .activated(true)
                    .locked(false)
                    .failedLoginCount(0)
                    .showCaptcha(false)
                    .emailVerified(true)
                    .mobileVerified(false)
                    .userSetting(userSetting)
                    .userRoles(List.of(userRole))
                    .createdDate(Instant.now())
                    .build();
            userSetting.setUser(existingUser);
            userRole.setUser(existingUser);

            spyRegistrationService = spy(registrationService);
            lenient().doReturn(decodedRegistration).when(spyRegistrationService)
                    .decodeGoogleRegistrationFromIdToken(providerDto.getToken(), providerDto.getProviderClientId());
            lenient().when(userMapper.maptoUser(any(RegistrationDto.class)))
                    .then(invocationOnMock -> buildMappedUser(invocationOnMock.getArgument(0)));
        }

        @Test
        void registerByAnotherProvider_whenExistingManualUser_thenReturnSameUserWithoutResettingPassword() {
            when(userService.getUserByEmail(decodedRegistration.getEmail())).thenReturn(Optional.of(existingUser));
            User returnedUser = spyRegistrationService.registerByAnotherProvider(providerDto);

            assertSame(existingUser, returnedUser);
            verify(userService, never()).saveUser(any(User.class));
        }

        @Test
        void registerByAnotherProvider_whenExistingSocialUserWithAnotherProvider_thenReturnSameUserWithoutChangingProvider() {
            existingUser.setAuthProvider(AuthProviderType.FACEBOOK);
            when(userService.getUserByEmail(decodedRegistration.getEmail())).thenReturn(Optional.of(existingUser));
            User returnedUser = spyRegistrationService.registerByAnotherProvider(providerDto);

            assertSame(existingUser, returnedUser);
            assertEquals(AuthProviderType.FACEBOOK, existingUser.getAuthProvider());
            verify(userService, never()).saveUser(any(User.class));
        }

        @Test
        void registerByAnotherProvider_whenExistingUserHasInvalidState_thenThrowException() {
            existingUser.setActivated(false);
            when(userService.getUserByEmail(decodedRegistration.getEmail())).thenReturn(Optional.of(existingUser));
            BusinessLogicException exception = assertThrows(BusinessLogicException.class,
                    () -> spyRegistrationService.registerByAnotherProvider(providerDto));

            assertEquals((int) ErrorEnum.USER_INVALID_STATE.getCode(), exception.getErrorCode());
        }

        @Test
        void registerByAnotherProvider_whenNewUser_thenCreateUserWithoutExposingStoredPassword() {
            when(userService.getUserByEmail(decodedRegistration.getEmail())).thenReturn(Optional.empty());
            when(localizationSupportedLangRepository.findByLanguageCode(anyString()))
                    .thenReturn(Optional.of(LocalizationSupportedLang.builder()
                            .languageCode("en")
                            .createdDate(Instant.now())
                            .build()));
            when(localizationSupportedCountryRepository.findByCountryCode(anyString()))
                    .thenReturn(Optional.of(LocalizationSupportedCountry.builder()
                            .countryCode("tr")
                            .createdDate(Instant.now())
                            .build()));
            doAnswer(invocation -> invocation.getArgument(0)).when(userService).saveUser(any(User.class));
            doAnswer(invocation -> invocation.getArgument(0)).when(userRoleService).create(any(UserRole.class));
            User returnedUser = spyRegistrationService.registerByAnotherProvider(providerDto);

            assertEquals(decodedRegistration.getEmail(), returnedUser.getEmail());
            assertTrue(returnedUser.isEmailVerified());
            assertFalse(new BCryptPasswordEncoder().matches(decodedRegistration.getPassword(), returnedUser.getPassword()));
        }

        @Test
        void registerByAnotherProvider_whenFacebookAccessToken_thenFetchUserDataInsteadOfDecodingTokenPayload() {
            providerDto.setProvider(AuthProviderType.FACEBOOK);
            decodedRegistration.setAuthProvider(AuthProviderType.FACEBOOK);
            when(userService.getUserByEmail(decodedRegistration.getEmail())).thenReturn(Optional.of(existingUser));
            doReturn(decodedRegistration).when(spyRegistrationService).decodeFacebookRegistration(providerDto);

            User returnedUser = spyRegistrationService.registerByAnotherProvider(providerDto);

            assertSame(existingUser, returnedUser);
            verify(spyRegistrationService).decodeFacebookRegistration(providerDto);
            verify(spyRegistrationService, never())
                    .decodeGoogleRegistration(providerDto);
        }

        @Test
        void decodeFacebookRegistration_whenAccessTokenProvided_thenMapFetchedFacebookUser() {
            providerDto.setProvider(AuthProviderType.FACEBOOK);
            spyRegistrationService = spy(registrationService);
            doReturn(Map.of(
                    "email", "facebook.user@example.com",
                    "first_name", "Facebook",
                    "last_name", "User"
            )).when(spyRegistrationService).fetchFacebookUserData(providerDto.getToken());

            RegistrationDto returnedRegistration = spyRegistrationService.decodeFacebookRegistration(providerDto);

            assertEquals("facebook.user@example.com", returnedRegistration.getEmail());
            assertEquals("Facebook", returnedRegistration.getName());
            assertEquals("User", returnedRegistration.getSurname());
            assertEquals(AuthProviderType.FACEBOOK, returnedRegistration.getAuthProvider());
        }

        @Test
        void registerByAnotherProvider_whenGoogleAuthorizationCode_thenExchangeCodeOnBackend() {
            providerDto.setToken(null);
            providerDto.setAuthorizationCode("auth-code");
            when(userService.getUserByEmail(decodedRegistration.getEmail())).thenReturn(Optional.of(existingUser));
            doReturn("google-id-token").when(spyRegistrationService).exchangeGoogleAuthorizationCodeForIdToken(providerDto);
            doReturn(decodedRegistration).when(spyRegistrationService)
                    .decodeGoogleRegistrationFromIdToken("google-id-token", providerDto.getProviderClientId());

            User returnedUser = spyRegistrationService.registerByAnotherProvider(providerDto);

            assertSame(existingUser, returnedUser);
            verify(spyRegistrationService).decodeGoogleRegistration(providerDto);
            verify(spyRegistrationService).exchangeGoogleAuthorizationCodeForIdToken(providerDto);
        }

        @Test
        void decodeFacebookRegistration_whenAuthorizationCodeProvided_thenExchangeCodeBeforeFetchingUser() {
            providerDto.setProvider(AuthProviderType.FACEBOOK);
            providerDto.setToken(null);
            providerDto.setAuthorizationCode("facebook-code");
            spyRegistrationService = spy(registrationService);
            doReturn("facebook-access-token").when(spyRegistrationService).exchangeFacebookAuthorizationCodeForAccessToken(providerDto);
            doReturn(Map.of(
                    "email", "facebook.user@example.com",
                    "first_name", "Facebook",
                    "last_name", "User"
            )).when(spyRegistrationService).fetchFacebookUserData("facebook-access-token");

            RegistrationDto returnedRegistration = spyRegistrationService.decodeFacebookRegistration(providerDto);

            assertEquals("facebook.user@example.com", returnedRegistration.getEmail());
            verify(spyRegistrationService).exchangeFacebookAuthorizationCodeForAccessToken(providerDto);
            verify(spyRegistrationService).fetchFacebookUserData("facebook-access-token");
        }
    }


}
