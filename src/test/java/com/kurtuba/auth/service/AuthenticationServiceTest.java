package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticate_rejectsBlockedUserBeforePasswordValidation() {
        User user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .password("$2a$10$invalidplaceholderhashinvalidplaceholde")
                .blocked(true)
                .build();

        when(userService.getUserByEmailOrMobile("user@example.com")).thenReturn(Optional.of(user));

        BusinessLogicException ex = assertThrows(BusinessLogicException.class,
                () -> authenticationService.authenticate("user@example.com", "secret"));

        assertEquals(ErrorEnum.USER_BLOCKED.getCode(), ex.getErrorCode());
    }
}
