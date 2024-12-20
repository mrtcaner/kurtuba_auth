package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.model.dto.EmailValidationDto;
import com.kurtuba.auth.data.model.dto.UserRegistrationDto;
import com.kurtuba.auth.data.model.dto.UserRegistrationOtherProviderDto;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.utils.AutoLoginUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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


    @GetMapping("/register/email/validation/link/{code}")
    private ModelAndView validateEmailByLink(@NotEmpty @PathVariable String code) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            userService.validateEmailByLink(code);
            modelAndView.setViewName("emailValidationSuccess.html");
        } catch (BusinessLogicException ex) {
            modelAndView.setViewName("emailValidationFailure.html");
            modelAndView.addObject("errorMessage", ex.getMessage());
        }
        return modelAndView;
    }

    @PostMapping("/register/email/validation/code")
    @ResponseBody
    private ResponseEntity validateEmailByCode(@Valid @RequestBody EmailValidationDto validationDto) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200))
                .body(userService.validateEmailByCode(validationDto.getEmail(), validationDto.getCode()));
    }

    @PutMapping("/register/email/validation/code/{email}")
    @ResponseBody
    private ResponseEntity resendValidationCode(@NotNull @PathVariable String email) {
        userService.resendValidationCode(email, true);
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body("success");
    }

    @PutMapping("/register/email/validation/link/{email}")
    @ResponseBody
    private ResponseEntity resendValidationLink(@NotNull @PathVariable String email) {
        userService.resendValidationCode(email, false);
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
