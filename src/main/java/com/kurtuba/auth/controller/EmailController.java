package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.EmailValidationDto;
import com.kurtuba.auth.data.dto.ResultPageDto;
import com.kurtuba.auth.error.enums.ErrorEnum;
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

    @GetMapping("/email/validation/link/{linkParam}")
    private ModelAndView validateEmailByLink(@NotEmpty @PathVariable String linkParam) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            userService.validateEmailByLink(linkParam);
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
    private ResponseEntity validateEmailByCode(@Valid @RequestBody EmailValidationDto validationDto) {
        try {
            userService.validateEmailByCode(validationDto.getUserMetaChangeId(), validationDto.getCode());
        } catch (BusinessLogicException e) {
            if (ErrorEnum.USER_META_CHANGE_CODE_MISMATCH.getCode().equals(e.getErrorCode())) {
                userService.updateEmailChangeTryCount(validationDto);
            }
            throw e;
        }
        return ResponseEntity.status(HttpStatusCode.valueOf(org.eclipse.jetty.http.HttpStatus.OK_200))
                .body("");
    }

    @PutMapping("/email/validation/code/{email}")
    private ResponseEntity resendRegistrationEmailValidationCode(@NotEmpty @PathVariable String email) {
        userService.sendRegistrationEmailValidationCode(email, true);
        return ResponseEntity.status(HttpStatusCode.valueOf(org.eclipse.jetty.http.HttpStatus.OK_200)).body("success");
    }

    @PutMapping("/email/validation/link/{email}")
    private ResponseEntity resendRegistrationEmailValidationLink(@NotEmpty @PathVariable String email) {
        userService.sendRegistrationEmailValidationCode(email, false);
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body("success");
    }
}