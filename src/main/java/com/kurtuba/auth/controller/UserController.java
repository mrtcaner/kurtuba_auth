package com.kurtuba.auth.controller;


import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.JWTClaimType;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;


@RestController
@RequestMapping("user")
public class UserController {

    final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * this method is for internal use only
     * token must have SERVICE in scope claim
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE')")
    public ResponseEntity getUserById(@PathVariable @NotEmpty String id) {
        return ResponseEntity.status(HttpStatus.OK_200).body(UserDto.fromUser(userService.getUserById(id).orElseThrow(
                () -> new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST))));
    }

    /**
     * users with a valid token can access
     * todo only certain info must be shared through a DTO
     *
     * @param authentication
     * @return
     */
    @GetMapping("/info")
    public ResponseEntity getUserInfo(JwtAuthenticationToken authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED_401).build();
        }
        if (authentication.getAuthorities().contains(JWTClaimType.SCOPE.name() + "_" + AuthoritiesType.SERVICE.name())) {
            // SERVICEs are not users
            return ResponseEntity.status(HttpStatus.BAD_REQUEST_400).build();
        }
        return ResponseEntity.status(HttpStatus.OK_200)
                .body(UserDto.fromUser(userService.getUserById(authentication.getName())
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST)))
        );
    }

    /**
     * Password change endpoint for logged-in users
     *
     * @param passwordChangeDto
     * @param principal
     * @return
     */
    @PutMapping("/password")
    public ResponseEntity changePassword(@Valid @RequestBody PasswordChangeDto passwordChangeDto, Principal principal) {
        userService.changePassword(passwordChangeDto, principal.getName());
        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

    /**
     * Creates a password reset request byCode or Link
     *
     * @param passwordResetRequestDto
     * @return
     */
    @PostMapping("/password/reset")
    public ResponseEntity requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto passwordResetRequestDto) {
        return ResponseEntity.status(HttpStatus.OK_200).body(UserMetaChangeDto.builder()
                .userMetaChangeId(userService.requestResetPassword(passwordResetRequestDto.getEmailMobile(),
                        passwordResetRequestDto.isByCode()).getId())
                .build());
    }

    /**
     * Receives valid reset code, userMetaChangeId, new password and client credentials
     * <p>
     * if client credentials are provided then return token(s)
     * else empty string
     *
     * @param passwordResetByCodeDto
     * @return
     */
    @PutMapping("/password/reset/code")
    public ResponseEntity resetPasswordByCode(@Valid @RequestBody PasswordResetByCodeDto passwordResetByCodeDto) {
        try {
            return ResponseEntity.status(HttpStatus.OK_200).body(userService.resetPasswordByCode(passwordResetByCodeDto));
        } catch (BusinessLogicException e) {
            if (ErrorEnum.USER_META_CHANGE_CODE_MISMATCH.getCode().equals(e.getErrorCode())) {
                userService.updatePasswordResetTryCount(passwordResetByCodeDto);
            }
            throw e;
        }
    }

    /**
     * Returns password change page upon receiving a valid password reset linkParam
     *
     * @param linkParam
     * @return
     */
    @GetMapping("/password/reset/password-reset/{linkParam}")
    public ModelAndView getPasswordResetPage(@Valid @PathVariable String linkParam) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            userService.validatePasswordResetLinkParam(linkParam);
            modelAndView.setViewName("passwordReset.html");
            modelAndView.addObject("passwordResetByLinkDto", PasswordResetByLinkDto.builder()
                    .linkParam(linkParam)
                    .build());
        } catch (BusinessLogicException ex) {
            modelAndView.setViewName("genericResult.html");//failure
            modelAndView.addAllObjects(ResultPageDto.builder()
                    .success(false)
                    .title("Password Reset Failed!")
                    .message1(ex.getMessage())
                    .build().toMap());
        }
        return modelAndView;
    }

    /**
     * Handles password-reset page form post and changes password
     *
     * @param passwordResetByLinkDto
     * @param result
     * @return
     */
    @PostMapping("/password/reset/password-reset")
    public ModelAndView handleResetPasswordPagePost(@Valid PasswordResetByLinkDto passwordResetByLinkDto, BindingResult result) {
        ModelAndView modelAndView = new ModelAndView();
        if (result.hasErrors()) {
            //in case password mismatch or required complexity is not fulfilled etc.
            modelAndView.setViewName("passwordReset.html");
            modelAndView.addObject("passwordResetByLinkDto", passwordResetByLinkDto);
            modelAndView.addAllObjects(result.getModel());
        } else {
            try {
                userService.resetPasswordByLink(passwordResetByLinkDto);
                modelAndView.setViewName("genericResult.html");//success
                modelAndView.addAllObjects(ResultPageDto.builder()
                        .success(true)
                        .title("Password changed successfully!")
                        .build().toMap());
            } catch (BusinessLogicException ex) {
                // user doesn't exist in the system or code expired etc.
                modelAndView.setViewName("genericResult.html");//failure
                modelAndView.addAllObjects(ResultPageDto.builder()
                        .success(false)
                        .title("Password Reset Failed!")
                        .message1(ex.getMessage())
                        .build().toMap());
            }
        }

        return modelAndView;

    }

    /**
     * Opens forgot-password page. User is expected to provide an email address to receive code/link to initiate
     * password reset
     *
     * @return
     */
    @GetMapping("/password/reset/forgot-password")
    public ModelAndView getForgotPasswordPage() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("forgotPasswordForm", new PasswordResetRequestDto());
        modelAndView.setViewName("requestPasswordReset.html");
        return modelAndView;
    }

    /**
     * Handles forgot-password page form post. Sends code/link to given email address
     *
     * @param passwordResetRequestDto
     * @param result
     * @return
     */
    @PostMapping("/password/reset/forgot-password")
    public ModelAndView handleForgotPasswordPage(@ModelAttribute("forgotPasswordForm") @Valid PasswordResetRequestDto passwordResetRequestDto,
                                                 BindingResult result) {
        ModelAndView modelAndView = new ModelAndView();
        if (result.hasErrors()) {
            //in case of malformed email
            modelAndView.setViewName("requestPasswordReset.html");
            modelAndView.addObject("forgotPasswordForm", passwordResetRequestDto);
        } else {
            // mail well-formed
            try {
                userService.requestResetPassword(passwordResetRequestDto.getEmailMobile(), false);
            } catch (BusinessLogicException ex) {
                // mail doesn't exist in the system or user's mail is not validated
                result.rejectValue("email", "1000", ex.getMessage());
                modelAndView.setViewName("requestPasswordReset.html");
                modelAndView.addObject("forgotPasswordForm", passwordResetRequestDto);
                modelAndView.addAllObjects(result.getModel());
                return modelAndView;
            }
            // success!
            modelAndView.setViewName("genericResult.html");//success
            modelAndView.addAllObjects(ResultPageDto.builder()
                    .success(true)
                    .message1("We sent a password reset link to " + passwordResetRequestDto.getEmailMobile())
                    .build().toMap());
        }

        return modelAndView;
    }


    /**
     * Sends a verification code/link to given email address for logged-in user
     *
     * @param emailVerificationRequestDto
     * @param principal
     * @return
     */
    @PostMapping("/email/verification")
    public ResponseEntity sendEmailVerification(@Valid @RequestBody EmailVerificationRequestDto emailVerificationRequestDto,
                                                Principal principal) {
        return ResponseEntity.status(HttpStatus.OK_200)
                .body(UserMetaChangeDto.builder()
                        .userMetaChangeId(userService.requestChangeEmail(principal.getName(),
                                emailVerificationRequestDto.getEmail(), emailVerificationRequestDto.isByCode()).getId())
                        .build());

    }

    /**
     * Verifies email using code
     *
     * @param verificationDto
     * @return
     */
    @PutMapping("/email/verification/code")
    public ResponseEntity verifyEmailByCode(@Valid @RequestBody EmailVerificationDto verificationDto) {
        try {
            userService.verifyEmailByCode(verificationDto.getEmailMobile(), verificationDto.getCode());
            return ResponseEntity.status(HttpStatusCode.valueOf(org.eclipse.jetty.http.HttpStatus.OK_200))
                    .body("");
        } catch (BusinessLogicException e) {
            if (ErrorEnum.USER_META_CHANGE_CODE_MISMATCH.getCode().equals(e.getErrorCode())) {
                userService.updateEmailChangeTryCount(verificationDto);
            }
            throw e;
        }
    }

    /**
     * Verifies email using link parameter
     *
     * @param linkParam is Base64 encoded UUID
     * @return genericResult.html
     */
    @GetMapping("/email/verification/link/{linkParam}")
    public ModelAndView verifyEmailByLink(@NotEmpty @PathVariable String linkParam) {
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

}
