package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserTokenService userTokenService;
    private final AuthenticationService authenticationService;

    @Transactional
    public TokensResponseDto authenticateAndGetTokens(String emailMobile, String pass,
                                                      String registeredClientId, String registeredClientSecret) {
            return getTokensForAuthenticatedUser(authenticationService.authenticate(emailMobile, pass),
                    registeredClientId, registeredClientSecret);
    }

    @Transactional
    public TokensResponseDto authenticateAdminAndGetTokens(String emailMobile, String pass,
                                                           String registeredClientId, String registeredClientSecret) {
        User user = authenticationService.authenticate(emailMobile, pass);
        boolean hasAdminRole = user.getUserRoles() != null && user.getUserRoles().stream()
                .anyMatch(userRole -> AuthoritiesType.ADMIN.name().equals(userRole.getRole().getName()));
        if (!hasAdminRole) {
            throw new BusinessLogicException(ErrorEnum.AUTH_ACCESS_TOKEN_INVALID);
        }

        return getTokensForAuthenticatedUser(user, registeredClientId, registeredClientSecret);
    }

    @Transactional
    public TokensResponseDto getTokensForUser(User user, String registeredClientId, String registeredClientSecret) {
        return getTokensForAuthenticatedUser(user, registeredClientId, registeredClientSecret);
    }

    private TokensResponseDto getTokensForAuthenticatedUser(User user, String registeredClientId,
                                                            String registeredClientSecret) {
        return userTokenService.validateRegisteredClientAndGetTokens(user, registeredClientId, registeredClientSecret);
    }


}
