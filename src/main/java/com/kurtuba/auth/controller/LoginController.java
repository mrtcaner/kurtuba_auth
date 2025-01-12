package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.LoginCredentialsDto;
import com.kurtuba.auth.data.dto.LoginServiceCredentialsDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.AuthenticationService;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.utils.TokenUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("auth")
public class LoginController {

    final
    UserService userService;

    final
    AuthenticationService authenticationService;

    final
    RegisteredClientRepository registeredClientRepository;

    final
    TokenUtils tokenUtils;

    public LoginController(UserService userService, AuthenticationService authenticationService, RegisteredClientRepository registeredClientRepository, TokenUtils tokenUtils) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.registeredClientRepository = registeredClientRepository;
        this.tokenUtils = tokenUtils;
    }

    @PostMapping("/login")
    public ResponseEntity login(@Valid @RequestBody LoginCredentialsDto loginCredentials) {
        if(!StringUtils.hasLength(loginCredentials.getClientId())){
            // if no client info is present then use default client
            RegisteredClient defaultClient = registeredClientRepository.findByClientType(RegisteredClientType.DEFAULT)
                    .get(0);
            loginCredentials.setClientId(defaultClient.getClientId());
            loginCredentials.setClientSecret(defaultClient.getClientSecret());
        }
        //throws exception if authentication fails
        //no exception means successful authentication. Generate token and return
        TokensResponseDto tokenDto = authenticationService.authenticateAndGetTokens(loginCredentials.getEmailUsername(),
                loginCredentials.getPassword(), loginCredentials.getClientId(), loginCredentials.getClientSecret());

        RegisteredClient client = registeredClientRepository.findByClientId(loginCredentials.getClientId())
                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID));
        if (client.isSendTokenInCookie()) {
            // return a cookie
            // HttpStatus-204
            ResponseCookie cookie = ResponseCookie.from("jwt", tokenDto.accessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(client.getCookieMaxAgeSeconds())
                    .build();

            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .build();
        } else {
            // return token json
            // HttpStatus-200
            return ResponseEntity.status(HttpStatus.OK)
                    .body(tokenDto);
        }


    }

    /**
     * Only used by services to get short-lived tokens
     * @param loginServiceCredentialsDto
     * @return
     */
    @PostMapping("/service/login")
    public ResponseEntity login(@Valid @RequestBody LoginServiceCredentialsDto loginServiceCredentialsDto) {
        RegisteredClient client = registeredClientRepository.findByClientId(loginServiceCredentialsDto.getClientId())
                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID));;
        if(!RegisteredClientType.SERVICE.equals(client.getClientType())){
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID);
        }
        if (!new BCryptPasswordEncoder().matches(loginServiceCredentialsDto.getClientSecret(), client.getClientSecret())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
        }

        // return token json
        return ResponseEntity.status(HttpStatus.OK)
                .body(TokensResponseDto.builder()
                        .accessToken(tokenUtils.generateToken(client.getId(), client.getAuds(),
                                client.getScopes(),
                                Duration.ofMinutes(client.getAccessTokenTtlMinutes())))
                        .build());

    }
}
