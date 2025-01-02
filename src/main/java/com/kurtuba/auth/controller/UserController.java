package com.kurtuba.auth.controller;


import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.JWTClaimType;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.utils.Utils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
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
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getUserById(id));
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED_401).body("");
        }
        if (authentication.getAuthorities().contains(JWTClaimType.SCOPE.name() + "_" + AuthoritiesType.SERVICE.name())) {
            // SERVICEs are not users
            return ResponseEntity.status(HttpStatus.BAD_REQUEST_400).body("");
        }
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getUserById(authentication.getName()));
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
     * Returns password change page upon receiving a valid password reset code
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

    @PostMapping("/password/reset/password-reset")
    public ModelAndView handleResetPassword(@Valid PasswordResetByLinkDto passwordResetByLinkDto, BindingResult result) {
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
     * Receives valid reset code(base64 UUID) and new password from password change page
     *
     * @param passwordResetByLinkDto
     * @return
     */
    @PutMapping("/password/reset/link")
    public ResponseEntity resetPasswordByLink(@Valid @RequestBody PasswordResetByLinkDto passwordResetByLinkDto) {
        userService.resetPasswordByLink(passwordResetByLinkDto);
        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

    /**
     * Receives valid reset code(6 digit random), userMetaChangeId and new password
     *
     * @param passwordResetByCodeDto
     * @return
     */
    @PutMapping("/password/reset/code")
    public ResponseEntity resetPasswordByCode(@Valid @RequestBody PasswordResetByCodeDto passwordResetByCodeDto) {
        try{
            userService.resetPasswordByCode(passwordResetByCodeDto);
        }catch (BusinessLogicException e){
            if(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH.getCode().equals(e.getErrorCode())){
                userService.updatePasswordResetTryCount(passwordResetByCodeDto);
            }
            throw e;
        }

        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

    @GetMapping("/password/reset/forgot-password")
    public ModelAndView getForgotPasswordPage() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("forgotPasswordForm", new ForgotPasswordDto());
        modelAndView.setViewName("requestPasswordReset.html");
        return modelAndView;
    }

    @PostMapping("/password/reset/forgot-password")
    public ModelAndView handleForgotPasswordPage(@ModelAttribute("forgotPasswordForm") @Valid ForgotPasswordDto form,
                                                 BindingResult result) {
        ModelAndView modelAndView = new ModelAndView();
        if (result.hasErrors()) {
            //in case of malformed email
            modelAndView.setViewName("requestPasswordReset.html");
            modelAndView.addObject("forgotPasswordForm", form);
        } else {
            // mail well-formed
            try {
                userService.requestResetPassword(form.getEmail(), false);
            } catch (BusinessLogicException ex) {
                // mail doesn't exist in the system or user's mail is not validated
                result.rejectValue("email", "1000", ex.getMessage());
                modelAndView.setViewName("requestPasswordReset.html");
                modelAndView.addObject("forgotPasswordForm", form);
                modelAndView.addAllObjects(result.getModel());
                return modelAndView;
            }
            // success!
            modelAndView.setViewName("genericResult.html");//success
            modelAndView.addAllObjects(ResultPageDto.builder()
                    .success(true)
                    .message1("We sent a password reset link to " + form.getEmail())
                    .build().toMap());
        }

        return modelAndView;


    }


    /**
     * Sends reset code to user's email-address. User is expected to manually enter the code to a from
     *
     * @param email
     * @return
     */
    @PostMapping("/password/reset/code/{email}")
    public ResponseEntity requestPasswordResetByCode(@NotEmpty @PathVariable String email) {

        return ResponseEntity.status(HttpStatus.OK_200)
                .body(UserMetaChangeDto.builder()
                        .userMetaChangeId(userService.requestResetPassword(email, true).getId())
                        .build());
    }

    /**
     * Send a link to user's email address. Link opens password-reset page
     *
     * @param email
     * @return
     */
    @PostMapping("/password/reset/link/{email}")
    public ResponseEntity requestPasswordResetByLink(@NotEmpty @PathVariable String email) {
        userService.requestResetPassword(email, false);
        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

    /**
     * Email change by validation-Code endpoint for logged-in users
     *
     * @param email
     * @param principal
     * @return
     */
    @PostMapping("/email/code/{email}")
    public ResponseEntity sendEmailValidationCode(@Valid @Email(regexp = Utils.EMAIL_REGEX) @PathVariable String email,
                                                  Principal principal) {

        return ResponseEntity.status(HttpStatus.OK_200)
                .body(UserMetaChangeDto.builder()
                        .userMetaChangeId(userService.requestChangeEmail(principal.getName(), email, true).getId())
                        .build());


    }

    /**
     * Email change by validation-Link endpoint for logged-in users
     *
     * @param email
     * @param principal
     * @return
     */
    @PostMapping("/email/link/{email}")
    public ResponseEntity sendEmailValidationLink(@Valid @Email(regexp = Utils.EMAIL_REGEX) @PathVariable String email,
                                                  Principal principal) {
        userService.requestChangeEmail(principal.getName(), email, false);
        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

}
