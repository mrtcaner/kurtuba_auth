package com.kurtuba.auth.service;

import com.kurtuba.auth.data.mapper.UserMapper;
import com.kurtuba.auth.data.model.LocalizationSupportedLang;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserSetting;
import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.data.repository.UserFcmTokenRepository;
import com.kurtuba.auth.data.repository.UserRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.ServiceUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTokenService userTokenService;

    @Mock
    private MessageJobService messageJobService;

    @Mock
    private UserMetaChangeService userMetaChangeService;

    @Mock
    private ServiceUtils serviceUtils;

    @Mock
    private LocalizationSupportedCountryRepository localizationSupportedCountryRepository;

    @Mock
    private LocalizationSupportedLangRepository localizationSupportedLangRepository;

    @Mock
    private UserFcmTokenRepository userFcmTokenRepository;

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void updateUserLang_updatesUsersLanguageCode() {
        User user = User.builder()
                .id("user-1")
                .userSetting(UserSetting.builder().languageCode("tr").countryCode("tr").build())
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(localizationSupportedLangRepository.findByLanguageCode("en"))
                .thenReturn(Optional.of(LocalizationSupportedLang.builder()
                        .id("lang-en")
                        .languageCode("en")
                        .createdDate(Instant.now())
                        .build()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUserLang("user-1", "en");

        assertEquals("en", user.getUserSetting().getLanguageCode());
        assertEquals("tr", user.getUserSetting().getCountryCode());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserLang_normalizesLanguageCode() {
        User user = User.builder()
                .id("user-1")
                .userSetting(UserSetting.builder().languageCode("tr").countryCode("tr").build())
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(localizationSupportedLangRepository.findByLanguageCode("en"))
                .thenReturn(Optional.of(LocalizationSupportedLang.builder()
                        .id("lang-en")
                        .languageCode("en")
                        .createdDate(Instant.now())
                        .build()));

        userService.updateUserLang("user-1", " EN ");

        assertEquals("en", user.getUserSetting().getLanguageCode());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserLang_throwsUnsupportedLanguageWhenLanguageDoesNotExist() {
        User user = User.builder()
                .id("user-1")
                .userSetting(UserSetting.builder().languageCode("tr").countryCode("tr").build())
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(localizationSupportedLangRepository.findByLanguageCode("en"))
                .thenReturn(Optional.empty());

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userService.updateUserLang("user-1", "en"));

        assertEquals(ErrorEnum.LOCALIZATION_UNSUPPORTED_LANGUAGE.getMessage(), ex.getMessage());
    }

    @Test
    void updateAdminSecurityAndActivity_blocksExistingTokensWhenUserBecomesBlocked() {
        User user = User.builder()
                .id("user-1")
                .activated(true)
                .locked(false)
                .blocked(false)
                .failedLoginCount(0)
                .showCaptcha(false)
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateAdminSecurityAndActivity("user-1", true, false, true, false, 0);

        assertEquals(true, user.isBlocked());
        verify(userTokenService).blockUsersTokens("user-1");
    }

    @Test
    void requestResetPassword_rejectsBlockedUsers() {
        User user = User.builder()
                .id("user-1")
                .activated(true)
                .blocked(true)
                .email("user@example.com")
                .emailVerified(true)
                .build();

        when(userRepository.getUserByEmailOrMobile("user@example.com")).thenReturn(Optional.of(user));

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userService.requestResetPassword("user@example.com", true));

        assertEquals(ErrorEnum.USER_BLOCKED.getCode(), ex.getErrorCode());
        verify(userMetaChangeService, never()).create(any());
    }
}
