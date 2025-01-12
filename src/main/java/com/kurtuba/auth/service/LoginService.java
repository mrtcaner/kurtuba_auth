package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.TokensResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    final
    UserTokenService userTokenService;

    final
    AuthenticationService authenticationService;

    public LoginService(UserTokenService userTokenService, AuthenticationService authenticationService) {
        this.userTokenService = userTokenService;
        this.authenticationService = authenticationService;
    }

    @Transactional
    public TokensResponseDto authenticateAndGetTokens(String emailMobile, String pass,
                                                      String registeredClientId, String registeredClientSecret) {
            // authenticate user and get tokens
            return userTokenService.validateRegisteredClientAndGetTokens(
                    authenticationService.authenticate(emailMobile, pass), registeredClientId, registeredClientSecret);
    }


}
