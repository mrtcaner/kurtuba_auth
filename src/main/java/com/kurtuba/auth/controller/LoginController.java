package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.LoginCredentialsDto;
import com.kurtuba.auth.data.dto.LoginServiceCredentialsDto;
import com.kurtuba.auth.data.dto.TokenReturnDto;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
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
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("auth")
public class LoginController {

    final
    UserService userService;

    final
    RegisteredClientRepository registeredClientRepository;

    final
    TokenUtils tokenUtils;

    public LoginController(UserService userService, RegisteredClientRepository registeredClientRepository, TokenUtils tokenUtils) {
        this.userService = userService;
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
        TokenReturnDto tokenDto = userService.generateTokensForLoginByRestRequest(loginCredentials.getEmailUsername(),
                loginCredentials.getPassword(), loginCredentials.getClientId(), loginCredentials.getClientSecret());

        RegisteredClient client = registeredClientRepository.findByClientId(loginCredentials.getClientId());
        if (client.isSendTokenInCookie()) {
            // return a cookie
            ResponseCookie cookie = ResponseCookie.from("jwt", tokenDto.accessToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(client.getCookieMaxAgeSeconds())
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body("");
        } else {
            // return token json
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
        RegisteredClient client = registeredClientRepository.findByClientId(loginServiceCredentialsDto.getClientId());
        if(!RegisteredClientType.SERVICE.equals(client.getClientType())){
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID);
        }
        if (!new BCryptPasswordEncoder().matches(loginServiceCredentialsDto.getClientSecret(), client.getClientSecret())) {
            throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
        }

        // return token json
        return ResponseEntity.status(HttpStatus.OK)
                .body(TokenReturnDto.builder()
                        .accessToken(tokenUtils.generateToken(client.getId(), Set.of(client.getClientName()),
                                client.getScopes().stream().collect(Collectors.toSet()),
                                Duration.ofMinutes(client.getAccessTokenTtlMinutes())))
                        .build());

    }
}
