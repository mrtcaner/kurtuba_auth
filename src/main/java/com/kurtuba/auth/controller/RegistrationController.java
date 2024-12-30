package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.EmailValidationDto;
import com.kurtuba.auth.data.dto.ResultPageDto;
import com.kurtuba.auth.data.dto.UserRegistrationDto;
import com.kurtuba.auth.data.dto.UserRegistrationOtherProviderDto;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.utils.AutoLoginUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("auth")
public class RegistrationController {

    final
    UserService userService;

    final
    AutoLoginUtil autoLoginUtil;

    public RegistrationController(UserService userService, AutoLoginUtil autoLoginUtil) {
        this.userService = userService;
        this.autoLoginUtil = autoLoginUtil;
    }

    @PostMapping("/register")
    @ResponseBody
    private ResponseEntity register(@Valid @RequestBody UserRegistrationDto newUser) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201)).body(userService.register(newUser));
    }

    //todo generate token rather than faking login
    @PostMapping("/register/other-provider")
    @ResponseBody
    private ResponseEntity registerViaAnotherProvider(@Valid @RequestBody UserRegistrationOtherProviderDto newUser) {
        UserRegistrationDto dto = userService.registerByAnotherProvider(newUser);
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201)).body(autoLoginUtil.getAccessToken(dto.getEmail(), dto.getPassword()));
    }

    @GetMapping(value = "/register/username/available/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private ResponseEntity isUsernameAvailable(@NotEmpty @PathVariable String username) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isUsernameAvailable(username));
    }

    @GetMapping("/register/email/available/{email}")
    @ResponseBody
    private ResponseEntity isEmailAvailable(@NotEmpty @PathVariable String email) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isEmailAvailable(email));
    }


}
