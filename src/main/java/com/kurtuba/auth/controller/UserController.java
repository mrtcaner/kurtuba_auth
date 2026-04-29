package com.kurtuba.auth.controller;


import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.JWTClaimType;
import com.kurtuba.auth.data.enums.MessageJobStateType;
import com.kurtuba.auth.data.mapper.UserMapper;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.service.LogoutService;
import com.kurtuba.auth.service.MessageJobService;
import com.kurtuba.auth.service.UserTokenService;
import com.kurtuba.auth.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/auth/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final LogoutService logoutService;
    private final MessageJobService messageJobService;
    private final UserMapper userMapper;
    private final UserTokenService userTokenService;
    private final RegisteredClientRepository registeredClientRepository;


    /**
     * users with a valid token can access
     *
     * @param authentication
     * @return
     */
    @GetMapping("/info")
    public ResponseEntity getUserInfo(JwtAuthenticationToken authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED_401).build();
        }
        if (authentication.getAuthorities()
                          .contains(JWTClaimType.SCOPE.name() + "_" + AuthoritiesType.SERVICE.name())) {
            // SERVICEs are not users
            return ResponseEntity.status(HttpStatus.BAD_REQUEST_400).build();
        }
        return ResponseEntity.status(HttpStatus.OK_200)
                             .body(userMapper.mapToUserDto(userService.getUserById(authentication.getName())
                                                                      .orElseThrow(() -> new BusinessLogicException(
                                                                              ErrorEnum.USER_DOESNT_EXIST))));
    }

    /**
     * this method is for internal use only
     * token must have SERVICE in scope claim
     */
    @PutMapping("/info/users/basic")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE')")
    public ResponseEntity<UsersBasicResponseDto> getUserById(
            @NotNull @RequestBody UsersBasicRequestDto usersBasicRequestDto) {
        return ResponseEntity.status(HttpStatus.OK_200)
                             .body(UsersBasicResponseDto.builder()
                                                        .usersById(userService.getUsersByIds(usersBasicRequestDto.getUserIds())
                                                                              .stream()
                                                                              .map(userMapper::mapToUserBasicDto)
                                                                              .collect(Collectors.toMap(UserBasicDto::getId,
                                                                                                        userBasicDto -> userBasicDto,
                                                                                                        (existing, replacement) -> existing,
                                                                                                        LinkedHashMap::new)))
                                                        .build());
    }

    /**
     * this method is for internal use only
     * token must have SERVICE in scope claim
     */
    @GetMapping("/admin-users")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE')")
    public ResponseEntity<List<UserBasicDto>> getAdminUsers() {
        return ResponseEntity.status(HttpStatus.OK_200).body(userService.getActiveAdminUsers());
    }


    /**
     * users with a valid token can access
     *
     * @param authentication
     * @return
     */
    @GetMapping("/locale")
    public ResponseEntity<UserLocaleDto> getUserLocale(JwtAuthenticationToken authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED_401).build();
        }
        if (authentication.getAuthorities()
                          .contains(JWTClaimType.SCOPE.name() + "_" + AuthoritiesType.SERVICE.name())) {
            // SERVICEs are not users
            return ResponseEntity.status(HttpStatus.BAD_REQUEST_400).build();
        }
        User usr = userService.getUserById(authentication.getName())
                              .orElseThrow(() -> new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));
        return ResponseEntity.status(HttpStatus.OK_200)
                             .body(UserLocaleDto.builder()
                                                .langCode(usr.getUserSetting().getLanguageCode())
                                                .countryCode(usr.getUserSetting().getCountryCode())
                                                .build());
    }

    @PutMapping("/locale/users")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE')")
    public ResponseEntity<UsersLocalesResponseDto> getUserLocalesByUserIds(
            @NotNull @RequestBody UsersLocalesRequestDto usersLocalesRequestDto) {
        UsersLocalesResponseDto.UsersLocalesResponseDtoBuilder usersLocalesDtoBuilder =
                UsersLocalesResponseDto.builder();
        if (usersLocalesRequestDto.getUserIds().size() == 1) {
            User usr = userService.getUserById(usersLocalesRequestDto.getUserIds().getFirst())
                                  .orElseThrow(() -> new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));
            return ResponseEntity.status(HttpStatus.OK_200)
                                 .body(usersLocalesDtoBuilder.locales(
                                                                     Map.of(usersLocalesRequestDto.getUserIds().getFirst(), UserLocaleDto.builder()
                                                                                                                                         .langCode(
                                                                                                                                                 usr.getUserSetting()
                                                                                                                                                    .getLanguageCode())
                                                                                                                                         .countryCode(
                                                                                                                                                 usr.getUserSetting()
                                                                                                                                                    .getCountryCode())
                                                                                                                                         .build()))
                                                             .build());

        }

        return ResponseEntity.status(HttpStatus.OK_200)
                             .body(usersLocalesDtoBuilder.locales(
                                                                 userService.getUsersByIds(usersLocalesRequestDto.getUserIds())
                                                                            .stream()
                                                                            .collect(Collectors.toMap(User::getId,
                                                                                                      user -> UserLocaleDto.builder()
                                                                                                                                        .langCode(
                                                                                                                                                user.getUserSetting()
                                                                                                                                                    .getLanguageCode())
                                                                                                                                        .countryCode(
                                                                                                                                                user.getUserSetting()
                                                                                                                                                    .getCountryCode())
                                                                                                                                        .build())))
                                                         .build());
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
        return ResponseEntity.status(HttpStatus.NO_CONTENT_204).build();
    }

    /**
     * Creates a password reset request byCode or Link
     *
     * @param passwordResetRequestDto
     * @return
     */
    @PostMapping("/password/reset")
    public ResponseEntity requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto passwordResetRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED_201)
                             .body(UserMetaChangeDto.builder()
                                                    .userMetaChangeId(userService.requestResetPassword(
                                                            passwordResetRequestDto.getEmailMobile(),
                                                            passwordResetRequestDto.isByCode()).getId())
                                                    .build());
    }

    /**
     * Receives valid reset code, new password and client credentials
     * <p>
     * if client credentials are provided then return token(s)
     * else no content(204)
     *
     * @param passwordResetByCodeDto
     * @return
     */
    @PutMapping("/password/reset/code")
    public ResponseEntity resetPasswordByCode(@Valid @RequestBody PasswordResetByCodeDto passwordResetByCodeDto) {
        TokensResponseDto tokens = userService.resetPasswordByCode(passwordResetByCodeDto);
        if (tokens != null) {
            return ResponseEntity.status(HttpStatus.CREATED_201).body(tokens);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT_204).build();
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
            modelAndView.addObject("passwordResetByLinkDto",
                                   PasswordResetByLinkDto.builder().linkParam(linkParam).build());
        } catch (BusinessLogicException ex) {
            modelAndView.setViewName("genericResult.html");//failure
            modelAndView.addAllObjects(ResultPageDto.builder()
                                                    .success(false)
                                                    .title("Password Reset Failed!")
                                                    .message1(ex.getMessage())
                                                    .build()
                                                    .toMap());
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
    public ModelAndView handleResetPasswordPagePost(@Valid PasswordResetByLinkDto passwordResetByLinkDto,
                                                    BindingResult result) {
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
                modelAndView.addAllObjects(
                        ResultPageDto.builder().success(true).title("Password changed successfully!").build().toMap());
            } catch (BusinessLogicException ex) {
                // user doesn't exist in the system or code expired etc.
                modelAndView.setViewName("genericResult.html");//failure
                modelAndView.addAllObjects(ResultPageDto.builder()
                                                        .success(false)
                                                        .title("Password Reset Failed!")
                                                        .message1(ex.getMessage())
                                                        .build()
                                                        .toMap());
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
    public ModelAndView handleForgotPasswordPageRequestPasswordReset(
            @ModelAttribute("forgotPasswordForm") @Valid PasswordResetRequestDto passwordResetRequestDto,
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
                                                    .message1("We sent a password reset link to " +
                                                              passwordResetRequestDto.getEmailMobile())
                                                    .build()
                                                    .toMap());
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
    public ResponseEntity requestChangeEmail(
            @Valid @RequestBody EmailVerificationRequestDto emailVerificationRequestDto, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED_201)
                             .body(UserMetaChangeDto.builder()
                                                    .userMetaChangeId(
                                                            userService.requestChangeEmail(principal.getName(),
                                                                                           emailVerificationRequestDto.getEmail(),
                                                                                           emailVerificationRequestDto.isByCode())
                                                                       .getId())
                                                    .build());

    }

    /**
     * Verifies email using code
     *
     * @param verificationDto
     * @return
     */
    @PutMapping("/email/verification/code")
    public ResponseEntity verifyEmailByCode(@Valid @RequestBody EmailVerificationDto verificationDto,
                                            Principal principal) {
        userService.verifyEmailByCode(principal.getName(), verificationDto.getCode());
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies email using link parameter
     *
     * @param linkParam is Base64 encoded UUID
     * @return genericResult.html
     */
    @GetMapping("/email/verification/link/{linkParam}")
    public ModelAndView verifyEmailByLink(@NotBlank @PathVariable String linkParam) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            userService.verifyEmailByLink(linkParam);
            modelAndView.setViewName("genericResult.html");//sucess
            modelAndView.addAllObjects(ResultPageDto.builder()
                                                    .success(true)
                                                    .title("Congratulations!")
                                                    .message1("You can now log in to your account with your email " +
                                                              "address")
                                                    .build()
                                                    .toMap());
        } catch (BusinessLogicException ex) {
            modelAndView.setViewName("genericResult.html");//failure
            modelAndView.addAllObjects(ResultPageDto.builder()
                                                    .success(false)
                                                    .title("Verification Failed!")
                                                    .message1(ex.getMessage())
                                                    .message2("Try logging in to your account to request a new " +
                                                              "verification link")
                                                    .build()
                                                    .toMap());
        }
        return modelAndView;
    }

    /**
     * Sends a verification code to given mobile address for logged-in user
     *
     * @param mobileVerificationRequestDto
     * @param principal
     * @return
     */
    @PostMapping("/mobile/verification")
    public ResponseEntity requestChangeMobile(
            @Valid @RequestBody MobileVerificationRequestDto mobileVerificationRequestDto, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED_201)
                             .body(UserMetaChangeDto.builder()
                                                    .userMetaChangeId(
                                                            userService.requestChangeMobile(principal.getName(),
                                                                                            mobileVerificationRequestDto.getMobile())
                                                                       .getId())
                                                    .build());

    }

    @DeleteMapping("/email")
    public ResponseEntity<Void> deleteEmail(Principal principal) {
        userService.deleteContact(principal.getName(), ContactType.EMAIL);
        return ResponseEntity.status(HttpStatus.NO_CONTENT_204).build();
    }

    @DeleteMapping("/mobile")
    public ResponseEntity<Void> deleteMobile(Principal principal) {
        userService.deleteContact(principal.getName(), ContactType.MOBILE);
        return ResponseEntity.status(HttpStatus.NO_CONTENT_204).build();
    }

    /**
     * Verifies mobile using code
     *
     * @param verificationDto
     * @return
     */
    @PutMapping("/mobile/verification/code")
    public ResponseEntity verifyMobileByCode(@Valid @RequestBody MobileVerificationDto verificationDto,
                                             Principal principal) {
        userService.verifyMobileByCode(principal.getName(), verificationDto.getCode());
        return ResponseEntity.ok().build();
    }

    /**
     * Updates users personal info
     *
     * @param userPersonalInfoDto
     * @param principal
     * @return
     */
    @PutMapping("/personal-info")
    public ResponseEntity updatePersonalInfo(@Valid @RequestBody UserPersonalInfoDto userPersonalInfoDto,
                                             Principal principal) {
        userService.updateUserPersonalInfo(principal.getName(), userPersonalInfoDto);
        return ResponseEntity.status(HttpStatus.OK_200).build();

    }

    @PutMapping("/username")
    public ResponseEntity<Void> updateUsername(@Valid @RequestBody UsernameUpdateDto usernameUpdateDto,
                                               Principal principal) {
        userService.updateUsername(principal.getName(), usernameUpdateDto.getUsername());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/lang")
    public ResponseEntity<Void> updateLang(@NotBlank @RequestParam String langCode, Principal principal) {
        userService.updateUserLang(principal.getName(), langCode);
        return ResponseEntity.ok().build();

    }


    // Users can only see their own verification code status
    @GetMapping("/verification/send-status")
    public ResponseEntity<MessageJobStateType> getVerificationMessageStatus(
            @NotBlank @RequestParam String userMetaChangeId, Principal principal) {
        return ResponseEntity.ok(
                messageJobService.findByUserMetaChangeIdAndUserId(userMetaChangeId, principal.getName()));

    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> upsertFcmToken(@Valid @RequestBody FcmTokenUpsertRequestDto fcmTokenUpsertRequestDto,
                                               JwtAuthenticationToken authenticationToken) {
        userService.upsertUserFcmToken(authenticationToken.getName(), fcmTokenUpsertRequestDto.getFcmToken(),
                                       authenticationToken.getToken().getId(),
                                       fcmTokenUpsertRequestDto.getFirebaseInstallationId());
        return ResponseEntity.ok().build();

    }

    @GetMapping("/fcm-token")
    public ResponseEntity<List<UserFcmTokenResponseDto>> getUserFcmToken(JwtAuthenticationToken authentication) {
        List<UserFcmTokenResponseDto> fcmTokens = userService.getUserFcmTokens(authentication.getName());
        if (fcmTokens.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND_404).build();
        }
        return ResponseEntity.ok(fcmTokens);

    }

    /**
     * this method is for internal use only
     * token must have SERVICE in scope claim
     */
    @PostMapping("/fcm-token/users")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE')")
    public ResponseEntity<UsersFcmTokensResponseDto> getUserFcmTokensByUserIds(
            @RequestBody UsersFcmTokensRequestDto usersFcmTokensRequestDto) {
        Map<String, List<UserFcmTokenResponseDto>> fcmTokens = userService.getUsersFcmTokens(
                usersFcmTokensRequestDto.getUserIds());
        return ResponseEntity.ok(UsersFcmTokensResponseDto.builder().usersFcmTokens(fcmTokens).build());

    }

    /**
     * this method is for internal use only
     * token must have SERVICE in scope claim
     */
    @PutMapping("/fcm-token/users")
    @PreAuthorize("hasAuthority('SCOPE_SERVICE')")
    public ResponseEntity<Void> deleteUserFcmTokens(@RequestBody FcmTokensDeleteRequestDto fcmTokensDeleteRequestDto) {
        userService.deleteFcmTokens(fcmTokensDeleteRequestDto.getFcmTokens());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(JwtAuthenticationToken authentication) {
        logoutService.doLogout(authentication.getToken().getId());
        RegisteredClient client = userTokenService.findByJTI(authentication.getToken().getId())
                                                  .flatMap(userToken -> registeredClientRepository.findByClientId(
                                                          userToken.getClientId()))
                                                  .orElse(null);
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                                              .httpOnly(client == null || client.isCookieHttpOnly())
                                              .secure(client != null && client.isCookieSecure())
                                              .path("/")
                                              .maxAge(0)
                                              .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/logout/firebase")
    public ResponseEntity<Void> logoutFcm(@NotBlank @RequestParam String firebaseInstallationId,
                                          JwtAuthenticationToken authentication) {
        logoutService.doLogoutFcm(authentication.getToken().getSubject(), firebaseInstallationId);
        return ResponseEntity.ok().build();
    }

}
