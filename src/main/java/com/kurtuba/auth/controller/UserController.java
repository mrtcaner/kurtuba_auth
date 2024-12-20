package com.kurtuba.auth.controller;


import com.kurtuba.auth.data.model.AuthoritiesEnum;
import com.kurtuba.auth.data.model.JWTClaimsEnum;
import com.kurtuba.auth.data.model.dto.ForgotPasswordDto;
import com.kurtuba.auth.data.model.dto.PasswordChangeDto;
import com.kurtuba.auth.data.model.dto.PasswordResetDto;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
     * @param principal
     * @return
     */
    @GetMapping("/info")
    public ResponseEntity getUserInfo(JwtAuthenticationToken principal) {
        if(principal == null){
           return ResponseEntity.status(HttpStatus.UNAUTHORIZED_401).body("");
        }
        if(principal.getAuthorities().contains(JWTClaimsEnum.SCOPE.name() + "_" +AuthoritiesEnum.SERVICE.name())){
            // SERVICEs are not users
            return ResponseEntity.status(HttpStatus.BAD_REQUEST_400).body("");
        }
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getUserById(principal.getName()));
    }

    /**
     * Password change endpoint for logged-in users
     * @param passwordChangeDto
     * @param principal
     * @return
     */
    @PutMapping("/password")
    public ResponseEntity changePassword(@Valid @RequestBody PasswordChangeDto passwordChangeDto,Principal principal) {
        userService.changePassword(passwordChangeDto, principal.getName());
        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

    /**
     *  Returns password change page upon receiving a valid password reset code
     * @param code
     * @return
     */
    @GetMapping("/password-reset/{code}")
    public ModelAndView getPasswordResetPage(@Valid @PathVariable String code) {
        ModelAndView modelAndView = new ModelAndView();
        try{
            userService.validatePasswordResetCode(code);
            modelAndView.setViewName("passwordReset.html");
            modelAndView.addObject("passwordResetDto",PasswordResetDto.builder().code(code).build());
        }catch (BusinessLogicException ex){

            modelAndView.setViewName("passwordResetFailure.html");
            modelAndView.addObject("errorMessage", ex.getMessage());
        }
        return modelAndView;
    }

    @PostMapping("/password-reset")
    public ModelAndView handleResetPassword(@Valid PasswordResetDto passwordResetDto, BindingResult result){
        ModelAndView modelAndView = new ModelAndView();
        if(result.hasErrors()){
            //in case password mismatch or required complexity is not fulfilled etc.
            modelAndView.setViewName("passwordReset.html");
            modelAndView.addObject("passwordResetDto", passwordResetDto);
            modelAndView.addAllObjects(result.getModel());
        }else {
            try {
                userService.resetPasswordByLink(passwordResetDto);
                modelAndView.setViewName("passwordResetSuccess.html");
            } catch (BusinessLogicException | UsernameNotFoundException ex) {
                // user doesn't exist in the system or code expired etc.
                modelAndView.setViewName("passwordResetFailure.html");
            }
        }

        return modelAndView;

    }

    /**
     * Receives valid reset code and new password from password change page
     * @param passwordResetDto
     * @return
     */
    @PutMapping("/password/reset/link")
    public ResponseEntity resetPasswordByLink(@Valid @RequestBody PasswordResetDto passwordResetDto) {
        userService.resetPasswordByLink(passwordResetDto);
        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

    /**
     * Receives valid reset code, email-address and new password from mobile
     * @param passwordResetDto
     * @return
     */
    @PutMapping("/password/reset/code")
    public ResponseEntity resetPasswordByCode(@Valid @RequestBody PasswordResetDto passwordResetDto) {
        userService.resetPasswordByCode(passwordResetDto);
        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

    @GetMapping("/forgot-password")
    public ModelAndView getForgotPasswordPage(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("forgotPasswordForm", new ForgotPasswordDto());
        modelAndView.setViewName("requestPasswordReset.html");
        return modelAndView;
    }

    @PostMapping("/forgot-password")
    public ModelAndView handleForgotPasswordPage(@ModelAttribute("forgotPasswordForm") @Valid ForgotPasswordDto form,
                                                 BindingResult result){
        ModelAndView modelAndView = new ModelAndView();
        if(result.hasErrors()){
            //in case of malformed email
            modelAndView.setViewName("requestPasswordReset.html");
            modelAndView.addObject("forgotPasswordForm", form);
        }else{
            // mail well-formed
            try{
                userService.requestResetPassword(form.getEmail(), false);
            }catch (BusinessLogicException | UsernameNotFoundException ex){
                // mail doesn't exist in the system or user's mail is not validated
                result.rejectValue("email","1000",ex.getMessage());
                modelAndView.setViewName("requestPasswordReset.html");
                modelAndView.addObject("forgotPasswordForm", form);
                modelAndView.addAllObjects(result.getModel());
                return modelAndView;
            }
            // success!
            modelAndView.setViewName("requestPasswordResetSuccess.html");
            modelAndView.addObject("email", form.getEmail());
        }

        return modelAndView;


    }


    /**
     * Sends reset code to user's email-address. User is expected to manually enter the code to a from
     * @param usernameEmail
     * @return
     */
    @PostMapping("/password/reset/code/{usernameEmail}")
    public ResponseEntity requestPasswordResetByCode(@NotEmpty @PathVariable String usernameEmail) {
        userService.requestResetPassword(usernameEmail, true);
        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

    /**
     * Send a link to user's email address. Link opens password-reset page
     * @param usernameEmail
     * @return
     */
    @PostMapping("/password/reset/link/{usernameEmail}")
    public ResponseEntity requestPasswordResetByLink(@NotEmpty @PathVariable String usernameEmail) {
        userService.requestResetPassword(usernameEmail, false);
        return ResponseEntity.status(HttpStatus.OK_200).body("");
    }

}
