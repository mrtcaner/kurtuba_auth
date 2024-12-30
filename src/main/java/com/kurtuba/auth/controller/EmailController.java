package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.EmailValidationDto;
import com.kurtuba.auth.data.dto.ResultPageDto;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("auth")
public class EmailController {

    final
    UserService userService;

    public EmailController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/email/validation/link/{code}")
    private ModelAndView validateEmailByLink(@NotEmpty @PathVariable String code) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            userService.validateEmailByLink(code);
            modelAndView.setViewName("genericResult.html");//sucess
            modelAndView.addAllObjects(ResultPageDto.builder()
                    .success(true)
                    .title("Congratulations!")
                    .message1("You can now log in to your account with your email address")
                    .build().toMap());
        } catch (BusinessLogicException ex) {
            modelAndView.setViewName("genericResult.html");//failure
            modelAndView.addAllObjects(ResultPageDto.builder()
                    .success(false)
                    .title("Verification Failed!")
                    .message1(ex.getMessage())
                    .message2("Try logging in to your account to request a new validation link")
                    .build().toMap());
        }
        return modelAndView;
    }

    @PostMapping("/email/validation/code")
    @ResponseBody
    private ResponseEntity validateEmailByCode(@Valid @RequestBody EmailValidationDto validationDto) {
        return ResponseEntity.status(HttpStatusCode.valueOf(org.eclipse.jetty.http.HttpStatus.OK_200))
                .body(userService.validateEmailByCode(validationDto.getEmail(), validationDto.getCode()));
    }

    @PutMapping("/email/validation/code/{email}")
    @ResponseBody
    private ResponseEntity sendEmailValidationCode(@NotEmpty @PathVariable String email) {
        userService.sendRegistrationEmailValidationCode(email, true);
        return ResponseEntity.status(HttpStatusCode.valueOf(org.eclipse.jetty.http.HttpStatus.OK_200)).body("success");
    }

    @PutMapping("/email/validation/link/{email}")
    @ResponseBody
    private ResponseEntity sendEmailValidationLink(@NotEmpty @PathVariable String email) {
        userService.sendRegistrationEmailValidationCode(email, false);
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body("success");
    }
}