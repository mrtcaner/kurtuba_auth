package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.model.dto.EmailValidationDto;
import com.kurtuba.auth.data.model.dto.UserRegistrationOtherProviderDto;
import com.kurtuba.auth.data.model.dto.UserRegistrationDto;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.utils.AutoLoginUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class ResgistrationController {

    @Autowired
    UserService userService;

    @Autowired
    AutoLoginUtil autoLoginUtil;

    @PostMapping("/register")
    @ResponseBody
    private ResponseEntity register(@Valid @RequestBody UserRegistrationDto newUser) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201)).body(userService.register(newUser));
    }

    @PostMapping("/register/other-provider")
    @ResponseBody
    private ResponseEntity registerViaAnotherProvider(@Valid @RequestBody UserRegistrationOtherProviderDto newUser) {
        UserRegistrationDto dto = userService.registerByAnotherProvider(newUser);
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201)).body(autoLoginUtil.getAccessToken(dto.getEmail(),dto.getPassword()));
    }

    @PostMapping("/register/email/validation")
    @ResponseBody
    private ResponseEntity validateEmail(@Valid @RequestBody EmailValidationDto validationDto) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.validateEmail(validationDto.getEmail(), validationDto.getCode()));
    }

    @PutMapping("/register/email/validation/{email}")
    @ResponseBody
    private ResponseEntity resendValidationCode(@NotNull @PathVariable String email) {
        userService.resendValidationCode(email);
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body("success");
    }

    @GetMapping(value = "/register/username/available/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private ResponseEntity isUsernameAvailable(@NotNull @PathVariable String username) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isUsernameAvailable(username));
    }

    @GetMapping("/register/email/available/{email}")
    @ResponseBody
    private ResponseEntity isEmailAvailable(@NotNull @PathVariable String email) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isEmailAvailable(email));
    }


}
