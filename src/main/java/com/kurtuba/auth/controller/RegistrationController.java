package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.RegistrationResponseDto;
import com.kurtuba.auth.data.dto.TokenResponseDto;
import com.kurtuba.auth.data.dto.UserRegistrationDto;
import com.kurtuba.auth.data.dto.UserRegistrationOtherProviderDto;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.utils.Utils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class RegistrationController {

    final UserService userService;

    final RegisteredClientRepository registeredClientRepository;

    public RegistrationController(UserService userService, RegisteredClientRepository registeredClientRepository) {
        this.userService = userService;
        this.registeredClientRepository = registeredClientRepository;
    }

    @PostMapping("/register")
    private ResponseEntity register(@Valid @RequestBody UserRegistrationDto newUser) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201)).body(
                RegistrationResponseDto.builder()
                        .userMetaChangeId(userService.register(newUser))
                        .user(userService.getUserByEmail(newUser.getEmail()))
                        .build());
    }

    @PostMapping("/register/other-provider")
    private ResponseEntity registerViaAnotherProvider(@Valid @RequestBody UserRegistrationOtherProviderDto newUser) {
        UserRegistrationDto dto = userService.registerByAnotherProvider(newUser);
        TokenResponseDto tokenResponseDto = userService.generateTokensForLogin(dto.getEmail(), dto.getPassword(),
                registeredClientRepository.findByClientName("kurtuba-mobile-client").getClientId(), "");
        return ResponseEntity
                .status(HttpStatusCode.valueOf(HttpStatus.CREATED_201))
                .body(tokenResponseDto);
    }

    @GetMapping("/register/username/available/{username}")
    private ResponseEntity isUsernameAvailable(@NotEmpty @PathVariable String username) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isUsernameAvailable(username));
    }

    @GetMapping("/register/email/available/{email}")
    private ResponseEntity isEmailAvailable(@NotEmpty @Email(regexp = Utils.EMAIL_REGEX) @PathVariable String email) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isEmailAvailable(email));
    }


}
