package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.UserPersonalInfoDto;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.GenderType;
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
import com.kurtuba.auth.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void updateUserPersonalInfo_updatesUsernameWhenAllowed() {
        User user = User.builder()
                .id("user-1")
                .name("Old")
                .surname("Name")
                .userSetting(UserSetting.builder()
                        .canChangeUsername(true)
                        .languageCode("tr")
                        .countryCode("tr")
                        .build())
                .build();
        UserPersonalInfoDto dto = UserPersonalInfoDto.builder()
                .name("New")
                .surname("Name")
                .gender(GenderType.FEMALE)
                .birthdate("22/04/2026")
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUserPersonalInfo("user-1", dto);

        assertEquals("New", user.getName());
        assertEquals("Name", user.getSurname());
        assertEquals(GenderType.FEMALE, user.getGender());
        verify(userRepository).save(user);
    }

    @Test
    void updateUsername_updatesUsernameWhenAllowed() {
        User user = User.builder()
                .id("user-1")
                .name("Old")
                .username(null)
                .userSetting(UserSetting.builder()
                        .canChangeUsername(true)
                        .languageCode("tr")
                        .countryCode("tr")
                        .build())
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.getUserByUsername("new.user")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUsername("user-1", "new.user");

        assertEquals("new.user", user.getUsername());
        assertFalse(user.getUserSetting().isCanChangeUsername());
        verify(userRepository).save(user);
    }

    @Test
    void updateUsername_rejectsUsernameChangeWhenNotAllowed() {
        User user = User.builder()
                .id("user-1")
                .name("Old")
                .username("current.user")
                .userSetting(UserSetting.builder()
                        .canChangeUsername(false)
                        .languageCode("tr")
                        .countryCode("tr")
                        .build())
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userService.updateUsername("user-1", "new.user"));

        assertEquals(ErrorEnum.USER_USERNAME_CHANGE_NOT_ALLOWED.getCode(), ex.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUsername_rejectsUsernameChangeWhenUsernameExists() {
        User user = User.builder()
                .id("user-1")
                .name("Old")
                .username(null)
                .userSetting(UserSetting.builder()
                        .canChangeUsername(true)
                        .languageCode("tr")
                        .countryCode("tr")
                        .build())
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.getUserByUsername("taken.user"))
                .thenReturn(Optional.of(User.builder().id("user-2").username("taken.user").build()));

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userService.updateUsername("user-1", "taken.user"));

        assertEquals(ErrorEnum.USER_USERNAME_ALREADY_EXISTS.getCode(), ex.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void generateUniqueUsername_buildsValidRandomUsername() {
        User user = User.builder()
                .name("Ali Veli")
                .surname("Çalışkan")
                .email("ali@example.com")
                .build();

        when(userRepository.getUserByUsername(any(String.class))).thenReturn(Optional.empty());

        String username = userService.generateUniqueUsername();

        assertTrue(username.matches(Utils.USERNAME_REGEX));
        assertTrue(username.matches("user_[a-z]{2}[0-9]{6}"));
    }

    @Test
    void generateMissingUsernames_updatesUsersWithoutUsername() {
        User userOne = User.builder()
                .id("user-1")
                .name("First")
                .surname("User")
                .userSetting(UserSetting.builder().canChangeUsername(false).build())
                .build();
        User userTwo = User.builder().id("user-2").email("second@example.com").build();

        when(userRepository.findUsersWithoutUsername()).thenReturn(List.of(userOne, userTwo));
        when(userRepository.getUserByUsername(any(String.class))).thenReturn(Optional.empty());

        int generatedCount = userService.generateMissingUsernames();

        assertEquals(2, generatedCount);
        assertTrue(userOne.getUsername().matches(Utils.USERNAME_REGEX));
        assertTrue(userTwo.getUsername().matches(Utils.USERNAME_REGEX));
        assertTrue(userOne.getUserSetting().isCanChangeUsername());
        verify(userRepository).saveAll(List.of(userOne, userTwo));
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

    @Test
    void deleteContact_rejectsDeletingOnlyContact() {
        User user = User.builder()
                .id("user-1")
                .activated(true)
                .email("user@example.com")
                .emailVerified(false)
                .mobile(null)
                .mobileVerified(false)
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userService.deleteContact("user-1", ContactType.EMAIL));

        assertEquals(ErrorEnum.USER_CONTACT_DELETE_NOT_ALLOWED.getCode(), ex.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteContact_allowsDeletingEitherUnverifiedContactWhenBothAreUnverified() {
        User user = User.builder()
                .id("user-1")
                .activated(true)
                .email("user@example.com")
                .emailVerified(false)
                .mobile("+905122345678")
                .mobileVerified(false)
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteContact("user-1", ContactType.EMAIL);

        assertNull(user.getEmail());
        assertFalse(user.isEmailVerified());
        assertEquals("+905122345678", user.getMobile());
        verify(userMetaChangeService).deletePendingContactMetaChanges("user-1", ContactType.EMAIL);
        verify(userRepository).save(user);
    }

    @Test
    void deleteContact_rejectsDeletingOnlyVerifiedContact() {
        User user = User.builder()
                .id("user-1")
                .activated(true)
                .email("user@example.com")
                .emailVerified(true)
                .mobile("+905122345678")
                .mobileVerified(false)
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userService.deleteContact("user-1", ContactType.EMAIL));

        assertEquals(ErrorEnum.USER_CONTACT_DELETE_NOT_ALLOWED.getCode(), ex.getErrorCode());
        verify(userMetaChangeService, never()).deletePendingContactMetaChanges(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteContact_allowsDeletingUnverifiedContactWhenOtherContactIsVerified() {
        User user = User.builder()
                .id("user-1")
                .activated(true)
                .email("user@example.com")
                .emailVerified(false)
                .mobile("+905122345678")
                .mobileVerified(true)
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteContact("user-1", ContactType.EMAIL);

        assertNull(user.getEmail());
        assertFalse(user.isEmailVerified());
        assertEquals("+905122345678", user.getMobile());
        verify(userMetaChangeService).deletePendingContactMetaChanges("user-1", ContactType.EMAIL);
        verify(userRepository).save(user);
    }

    @Test
    void deleteContact_allowsDeletingOneVerifiedContactWhenBothContactsAreVerified() {
        User user = User.builder()
                .id("user-1")
                .activated(true)
                .email("user@example.com")
                .emailVerified(true)
                .mobile("+905122345678")
                .mobileVerified(true)
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteContact("user-1", ContactType.MOBILE);

        assertNull(user.getMobile());
        assertFalse(user.isMobileVerified());
        assertEquals("user@example.com", user.getEmail());
        verify(userMetaChangeService).deletePendingContactMetaChanges("user-1", ContactType.MOBILE);
        verify(userRepository).save(user);
    }

    @Test
    void deleteContact_rejectsDeletingMissingContact() {
        User user = User.builder()
                .id("user-1")
                .activated(true)
                .email("user@example.com")
                .emailVerified(true)
                .mobile(null)
                .mobileVerified(false)
                .build();

        when(userRepository.getUserById("user-1")).thenReturn(Optional.of(user));

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> userService.deleteContact("user-1", ContactType.MOBILE));

        assertEquals(ErrorEnum.USER_CONTACT_NOT_PRESENT.getCode(), ex.getErrorCode());
        verify(userMetaChangeService, never()).deletePendingContactMetaChanges(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }
}
