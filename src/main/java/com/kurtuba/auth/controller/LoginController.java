package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.LoginCredentialsDto;
import com.kurtuba.auth.data.dto.TokenReturnDto;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class LoginController {

    final
    UserService userService;

    final
    RegisteredClientRepository registeredClientRepository;

    public LoginController(UserService userService, RegisteredClientRepository registeredClientRepository) {
        this.userService = userService;
        this.registeredClientRepository = registeredClientRepository;
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity login(@Valid @RequestBody LoginCredentialsDto loginCredentials) {
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
}
