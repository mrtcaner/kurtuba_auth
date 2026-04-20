package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.mapper.UserMapper;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.model.User;
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

import java.util.List;

@RestController
@RequestMapping("/auth/registration")
public class RegistrationController {

    final UserService userService;

    final RegistrationService registrationService;

    final RegisteredClientRepository registeredClientRepository;

    final LoginService loginService;

    final UserMapper userMapper;

    public RegistrationController(UserService userService, RegistrationService registrationService,
                                  RegisteredClientRepository registeredClientRepository, LoginService loginService,
                                  UserMapper userMapper) {
        this.registrationService = registrationService;
        this.userService = userService;
        this.registeredClientRepository = registeredClientRepository;
        this.loginService = loginService;
        this.userMapper = userMapper;
    }

    @PostMapping("")
    @ApiResponse(responseCode = "201", description = "User created successfully", content = {@Content(mediaType =
            "application/json", schema = @Schema(implementation = RegistrationResponseDto.class))})
    public ResponseEntity register(@Valid @RequestBody RegistrationDto newUser) {
        UserMetaChange userMetaChange = registrationService.register(newUser);
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201))
                             .body(RegistrationResponseDto.builder()
                                                          .userMetaChangeId(userMetaChange.getId())
                                                          .user(userMapper.mapToUserDto(
                                                                  userService.getUserById(userMetaChange.getUserId())
                                                                             .orElseThrow(
                                                                                     () -> new BusinessLogicException(
                                                                                             ErrorEnum.USER_DOESNT_EXIST))))
                                                          .build());
    }

    @GetMapping("/locales")
    public ResponseEntity<AvailableLocalizationOptionsDto> getAvailableLocales() {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200))
                             .body(registrationService.getAvailableLocales());
    }

    @ApiResponse(responseCode = "201", description = "User created successfully", content = {@Content(mediaType =
            "application/json", schema = @Schema(implementation = TokensResponseDto.class))})
    @PostMapping("/other-provider")
    public ResponseEntity<TokensResponseDto> registerViaAnotherProvider(
            @Valid @RequestBody RegistrationOtherProviderDto newUser) {
        User user = registrationService.registerByAnotherProvider(newUser);
        String registeredClientId;
        String registeredClientSecret;
        if (newUser.getRegisteredClientId() != null) {
            registeredClientId = newUser.getRegisteredClientId();
            registeredClientSecret = newUser.getRegisteredClientSecret();
        } else {
            List<RegisteredClient> defaultClientList = registeredClientRepository.findByClientType(
                    RegisteredClientType.DEFAULT);
            if (defaultClientList.isEmpty()) {
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID);
            }
            registeredClientId = defaultClientList.getFirst().getClientId();
            registeredClientSecret = defaultClientList.getFirst().getClientSecret();
        }

        TokensResponseDto tokenResponseDto = loginService.getTokensForUser(user, registeredClientId,
                                                                           registeredClientSecret);
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201)).body(tokenResponseDto);
    }

    @ApiResponse(responseCode = "200", description = "Boolean result", content = {@Content(mediaType = "application" +
                                                                                                       "/json",
            schema = @Schema(implementation = Boolean.class))})
    @GetMapping("/username/available/{username}")
    public ResponseEntity<Boolean> isUsernameAvailable(@NotBlank @PathVariable String username) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200))
                             .body(userService.isUsernameAvailable(username));
    }

    @ApiResponse(responseCode = "200", description = "Boolean result", content = {@Content(mediaType = "application" +
                                                                                                       "/json",
            schema = @Schema(implementation = Boolean.class))})
    @GetMapping("/email/available/{email}")
    public ResponseEntity<Boolean> isEmailAvailable(
            @NotBlank @Email(regexp = Utils.EMAIL_REGEX) @PathVariable String email) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200))
                             .body(userService.isEmailAvailable(email));
    }

    @ApiResponse(responseCode = "200", description = "Boolean result", content = {@Content(mediaType = "application" +
                                                                                                       "/json",
            schema = @Schema(implementation = Boolean.class))})
    @GetMapping("/mobile/available/{mobile}")
    public ResponseEntity<Boolean> isMobileAvailable(@NotBlank @PathVariable String mobile) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200))
                             .body(userService.isMobileAvailable(mobile));
    }

    /**
     * Resends activation CODE/LINK to the given contact
     */
    @ApiResponse(responseCode = "201", description = "activation CODE/LINK sent", content = {@Content(mediaType =
            "application/json", schema = @Schema(implementation = UserMetaChangeDto.class))})
    @PostMapping("/activation")
    public ResponseEntity<UserMetaChangeDto> resendAccountActivationMessage(
            @Valid @RequestBody AccountActivationRequestDto accountActivationRequestDto) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201))
                             .body(UserMetaChangeDto.builder()
                                                    .userMetaChangeId(registrationService.sendAccountActivationMessage(
                                                            accountActivationRequestDto.getEmailMobile(),
                                                            accountActivationRequestDto.isByCode()).getId())
                                                    .build());
    }

    /**
     * Activates user account and verifies the contact info(email or mobile)
     * if client credentials provided in the request then upon successful activation returns token(s)
     * else empty string
     */
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "User activated", content =
            {@Content(mediaType = "application/json", schema = @Schema(implementation = Void.class))}),
            @ApiResponse(responseCode = "201", description = "User activated and tokens created", content =
                    {@Content(mediaType = "application/json", schema = @Schema(implementation =
                            TokensResponseDto.class))})})
    @PutMapping("/activation")
    public ResponseEntity activateAccountByCode(@Valid @RequestBody AccountActivationDto accountActivationDto) {
        TokensResponseDto tokens = registrationService.activateAccountByCode(accountActivationDto.getEmailMobile(),
                                                                             accountActivationDto.getCode(),
                                                                             accountActivationDto.getClientId(),
                                                                             accountActivationDto.getClientSecret());

        return tokens == null ? ResponseEntity.status(HttpStatus.OK_200).build()
                              : ResponseEntity.status(HttpStatus.CREATED_201).body(tokens);

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
                                                    .message1("You can now log in to your account with your " +
                                                              umc.getContactType().toString().toLowerCase())
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

}
