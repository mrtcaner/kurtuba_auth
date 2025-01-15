package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.LoginService;
import com.kurtuba.auth.service.RegistrationService;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.utils.Utils;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("registration")
public class RegistrationController {

    final UserService userService;

    final RegistrationService registrationService;

    final RegisteredClientRepository registeredClientRepository;

    final LoginService loginService;

    public RegistrationController(UserService userService, RegistrationService registrationService, RegisteredClientRepository registeredClientRepository, LoginService loginService) {
        this.registrationService = registrationService;
        this.userService = userService;
        this.registeredClientRepository = registeredClientRepository;
        this.loginService = loginService;
    }

    @PostMapping("")
    @ApiResponse(responseCode = "201", description = "User created successfully",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = RegistrationResponseDto.class))})
    public ResponseEntity register(@Valid @RequestBody RegistrationDto newUser) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201)).body(
                RegistrationResponseDto.builder()
                        .userMetaChangeId(registrationService.register(newUser))
                        .user(UserDto.fromUser(userService.getUserByEmail(newUser.getEmail()).orElseThrow(() ->
                                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST))))
                        .build());
    }

    @ApiResponse(responseCode = "201", description = "User created successfully",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = TokensResponseDto.class))})
    @PostMapping("/other-provider")
    public ResponseEntity<TokensResponseDto> registerViaAnotherProvider(@Valid @RequestBody RegistrationOtherProviderDto newUser) {
        RegistrationDto dto = registrationService.registerByAnotherProvider(newUser);
        TokensResponseDto tokenResponseDto = loginService.authenticateAndGetTokens(dto.getEmail(), dto.getPassword(),
                registeredClientRepository.findByClientType(RegisteredClientType.DEFAULT).get(0).getClientId(),
                "");
        return ResponseEntity
                .status(HttpStatusCode.valueOf(HttpStatus.CREATED_201))
                .body(tokenResponseDto);
    }

    @ApiResponse(responseCode = "200", description = "Boolean result",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class))})
    @GetMapping("/username/available/{username}")
    public ResponseEntity<Boolean> isUsernameAvailable(@NotBlank @PathVariable String username) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isUsernameAvailable(username));
    }

    @ApiResponse(responseCode = "200", description = "Boolean result",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class))})
    @GetMapping("/email/available/{email}")
    public ResponseEntity<Boolean> isEmailAvailable(@NotBlank @Email(regexp = Utils.EMAIL_REGEX) @PathVariable String email) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isEmailAvailable(email));
    }

    @ApiResponse(responseCode = "200", description = "Boolean result",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = Boolean.class))})
    @GetMapping("/mobile/available/{mobile}")
    public ResponseEntity<Boolean> isMobileAvailable(@NotBlank @PathVariable String mobile) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isMobileAvailable(mobile));
    }

    /**
     * Resends activation CODE/LINK to the given contact
     */
    @ApiResponse(responseCode = "201", description = "activation CODE/LINK sent",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserMetaChangeDto.class))})
    @PostMapping("/activation")
    public ResponseEntity<UserMetaChangeDto> resendAccountActivationLink(@Valid @RequestBody AccountActivationRequestDto accountActivationRequestDto) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201)).body(
                UserMetaChangeDto.builder().userMetaChangeId(userService.sendAccountActivationMessage(accountActivationRequestDto.getEmailMobile(),
                        accountActivationRequestDto.isByCode())).build()
        );
    }

    /**
     * Activates user account and verifies the contact info(email or mobile)
     * if client credentials provided in the request then upon successful activation returns token(s)
     * else empty string
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User activated",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Void.class))}),
            @ApiResponse(responseCode = "201", description = "User activated and tokens created",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = TokensResponseDto.class))})})
    @PutMapping("/activation")
    public ResponseEntity activateAccountByCode(@Valid @RequestBody AccountActivationDto accountActivationDto) {
        TokensResponseDto tokens = registrationService.activateAccountByCode(accountActivationDto.getEmailMobile(), accountActivationDto.getCode(),
                accountActivationDto.getClientId(), accountActivationDto.getClientSecret());

        return tokens == null ? ResponseEntity.status(HttpStatus.OK_200).build() :
                ResponseEntity.status(HttpStatus.CREATED_201).body(tokens);

    }

    /**
     * Called by user after clicking/tapping on activation link
     * Activates user account and verifies the contact info(email or mobile)
     * returns success/fail page
     */
    @GetMapping("/activation/link/{linkParam}")
    public ModelAndView activateAccountByLink(@NotBlank @PathVariable String linkParam) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            UserMetaChange umc = registrationService.activateAccountByLink(linkParam);
            modelAndView.setViewName("genericResult.html");//sucess
            modelAndView.addAllObjects(ResultPageDto.builder()
                    .success(true)
                    .title("Congratulations!")
                    .message1("You can now log in to your account with your " + umc.getContactType().toString().toLowerCase())
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
