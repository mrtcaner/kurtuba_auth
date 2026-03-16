package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .password(passwordEncoder.encode("correct-password"))
                .activated(true)
                .locked(false)
                .showCaptcha(false)
                .failedLoginCount(0)
                .lastLoginAttempt(Instant.now().minusSeconds(3600))
                .build();
    }

    @Test
    void authenticate_whenPasswordIsWrong_thenFailedCountIsIncrementedAndSaved() {
        when(userService.getUserByEmailOrMobile("user@example.com")).thenReturn(Optional.of(user));
        when(userService.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusinessLogicException exception = assertThrows(BusinessLogicException.class,
                () -> authenticationService.authenticate("user@example.com", "wrong-password"));

        assertEquals(ErrorEnum.LOGIN_INVALID_CREDENTIALS.getCode(), exception.getErrorCode());

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveUser(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();
        assertEquals(1, savedUser.getFailedLoginCount());
        assertFalse(savedUser.isLocked());
        assertFalse(savedUser.isShowCaptcha());
        assertNotNull(savedUser.getLastLoginAttempt());
    }

    @Test
    void authenticate_whenFifthAttemptFails_thenCaptchaIsEnabled() {
        user.setFailedLoginCount(4);
        when(userService.getUserByEmailOrMobile("user@example.com")).thenReturn(Optional.of(user));
        when(userService.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusinessLogicException exception = assertThrows(BusinessLogicException.class,
                () -> authenticationService.authenticate("user@example.com", "wrong-password"));

        assertEquals(ErrorEnum.LOGIN_INVALID_CREDENTIALS.getCode(), exception.getErrorCode());

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveUser(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();
        assertEquals(5, savedUser.getFailedLoginCount());
        assertTrue(savedUser.isShowCaptcha());
        assertFalse(savedUser.isLocked());
    }

    @Test
    void authenticate_whenTenthAttemptFails_thenAccountIsLocked() {
        user.setFailedLoginCount(9);
        when(userService.getUserByEmailOrMobile("user@example.com")).thenReturn(Optional.of(user));
        when(userService.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusinessLogicException exception = assertThrows(BusinessLogicException.class,
                () -> authenticationService.authenticate("user@example.com", "wrong-password"));

        assertEquals(ErrorEnum.LOGIN_USER_LOCKED.getCode(), exception.getErrorCode());
        assertTrue(exception.getMessage().startsWith("blockedUntil:"));

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveUser(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();
        assertEquals(10, savedUser.getFailedLoginCount());
        assertTrue(savedUser.isShowCaptcha());
        assertTrue(savedUser.isLocked());
    }

    @Test
    void authenticate_whenPasswordIsCorrect_thenCountersAreResetAndUserReturned() {
        user.setFailedLoginCount(6);
        user.setShowCaptcha(true);
        user.setLocked(true);
        when(userService.getUserByEmailOrMobile("user@example.com")).thenReturn(Optional.of(user));
        when(userService.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User authenticatedUser = authenticationService.authenticate("user@example.com", "correct-password");

        assertEquals(user, authenticatedUser);
        verify(userService).saveUser(user);
        assertEquals(0, authenticatedUser.getFailedLoginCount());
        assertFalse(authenticatedUser.isShowCaptcha());
        assertFalse(authenticatedUser.isLocked());
    }

    @Test
    void authenticate_whenAlreadyLockedAndBlockWindowNotExpired_thenThrowsWithoutSaving() {
        user.setLocked(true);
        user.setFailedLoginCount(10);
        user.setLastLoginAttempt(Instant.now());
        when(userService.getUserByEmailOrMobile("user@example.com")).thenReturn(Optional.of(user));

        BusinessLogicException exception = assertThrows(BusinessLogicException.class,
                () -> authenticationService.authenticate("user@example.com", "correct-password"));

        assertEquals(ErrorEnum.LOGIN_USER_LOCKED.getCode(), exception.getErrorCode());
        verify(userService, never()).saveUser(any(User.class));
    }
}
