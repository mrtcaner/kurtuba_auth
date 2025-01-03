package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.EmailVerificationDto;
import com.kurtuba.auth.data.dto.ResultPageDto;
import com.kurtuba.auth.data.dto.TokenResponseDto;
import com.kurtuba.auth.data.dto.UserDto;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserMetaChangeService;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.service.UserTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.util.Set;

@RestController
@RequestMapping("auth")
public class EmailVerificationController {

    final
    UserService userService;

    final
    UserTokenService userTokenService;

    final
    RegisteredClientRepository registeredClientRepository;

    final
    UserMetaChangeService userMetaChangeService;

    public EmailVerificationController(UserService userService, UserTokenService userTokenService,
                                       RegisteredClientRepository registeredClientRepository,
                                       UserMetaChangeService userMetaChangeService) {
        this.userService = userService;
        this.userTokenService = userTokenService;
        this.registeredClientRepository = registeredClientRepository;
        this.userMetaChangeService = userMetaChangeService;
    }

    @GetMapping("/email/verification/link/{linkParam}")
    private ModelAndView verifyEmailByLink(@NotEmpty @PathVariable String linkParam) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            userService.verifyEmailByLink(linkParam);
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
                    .message2("Try logging in to your account to request a new verification link")
                    .build().toMap());
        }
        return modelAndView;
    }

    @PostMapping("/email/verification/code")
    private ResponseEntity verifyEmailByCode(@Valid @RequestBody EmailVerificationDto verificationDto) {

        try {
            // check if this is account activation
            String userMetaChangeId = verificationDto.getUserMetaChangeId();
            boolean emailVerified = userService.getUserById(userMetaChangeService.findById(userMetaChangeId)
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.USER_EMAIL_VERIFICATION_CODE_INVALID))
                    .getUserId()).isEmailVerified();

            // verify email address
            UserDto user = userService.verifyEmailByCode(verificationDto.getUserMetaChangeId(), verificationDto.getCode());

            if(!emailVerified){
                // this is account activation
                // create tokens using default client settings and return
                RegisteredClient defaultClient = registeredClientRepository.findByClientType(RegisteredClientType.DEFAULT).get(0);
                TokenResponseDto tokenResponseDto = userTokenService.createAndSaveTokens(user.getId(), defaultClient.getClientId(),
                        Set.of(defaultClient.getClientName()), null, Duration.ofMinutes(defaultClient.getAccessTokenTtlMinutes()),
                        Duration.ofMinutes(defaultClient.getRefreshTokenTtlMinutes()));

                return ResponseEntity.status(HttpStatusCode.valueOf(org.eclipse.jetty.http.HttpStatus.OK_200))
                        .body(tokenResponseDto);
            }else{
                return ResponseEntity.status(HttpStatusCode.valueOf(org.eclipse.jetty.http.HttpStatus.OK_200))
                        .body(user);
            }




        } catch (BusinessLogicException e) {
            if (ErrorEnum.USER_META_CHANGE_CODE_MISMATCH.getCode().equals(e.getErrorCode())) {
                userService.updateEmailChangeTryCount(verificationDto);
            }
            throw e;
        }
    }

    @PutMapping("/email/registration/verification/code/{email}")
    private ResponseEntity resendAccountActivationCode(@NotEmpty @PathVariable String email) {
        userService.sendAccountActivationMail(email, true);
        return ResponseEntity.status(HttpStatusCode.valueOf(org.eclipse.jetty.http.HttpStatus.OK_200)).body("success");
    }

    @PutMapping("/email/registration/verification/link/{email}")
    private ResponseEntity resendAccountActivationLink(@NotEmpty @PathVariable String email) {
        userService.sendAccountActivationMail(email, false);
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body("success");
    }
}