package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.utils.Utils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("auth")
public class RegistrationController {

    final UserService userService;

    final RegisteredClientRepository registeredClientRepository;

    public RegistrationController(UserService userService, RegisteredClientRepository registeredClientRepository) {
        this.userService = userService;
        this.registeredClientRepository = registeredClientRepository;
    }

    @PostMapping("/registration")
    private ResponseEntity register(@Valid @RequestBody UserRegistrationDto newUser) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.CREATED_201)).body(
                RegistrationResponseDto.builder()
                        .userMetaChangeId(userService.register(newUser))
                        .user(userService.getUserByEmail(newUser.getEmail()))
                        .build());
    }

    @PostMapping("/registration/other-provider")
    private ResponseEntity registerViaAnotherProvider(@Valid @RequestBody UserRegistrationOtherProviderDto newUser) {
        UserRegistrationDto dto = userService.registerByAnotherProvider(newUser);
        TokenResponseDto tokenResponseDto = userService.generateTokensForLogin(dto.getEmail(), dto.getPassword(),
                registeredClientRepository.findByClientName("kurtuba-mobile-client").getClientId(), "");
        return ResponseEntity
                .status(HttpStatusCode.valueOf(HttpStatus.CREATED_201))
                .body(tokenResponseDto);
    }

    @GetMapping("/registration/username/available/{username}")
    private ResponseEntity isUsernameAvailable(@NotEmpty @PathVariable String username) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isUsernameAvailable(username));
    }

    @GetMapping("/registration/email/available/{email}")
    private ResponseEntity isEmailAvailable(@NotEmpty @Email(regexp = Utils.EMAIL_REGEX) @PathVariable String email) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isEmailAvailable(email));
    }

    @GetMapping("/registration/mobile/available/{mobile}")
    private ResponseEntity isMobileAvailable(@NotEmpty @PathVariable String mobile) {
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200)).body(userService.isMobileAvailable(mobile));
    }

    /**
     * Resends activation CODE/LINK to the given contact
     * @param accountActivationRequestDto
     * @return
     */
    @PutMapping("/registration/activation")
    private ResponseEntity resendAccountActivationLink(@Valid @RequestBody AccountActivationRequestDto accountActivationRequestDto) {
        userService.sendAccountActivationMessage(accountActivationRequestDto.getEmailMobile(),
                        accountActivationRequestDto.isByCode());
        return ResponseEntity.status(HttpStatusCode.valueOf(HttpStatus.OK_200))
                .body("");
    }

    /**
     * Activates user account and verifies the contact info(email or mobile)
     * if client credentials provided in the request then upon successful activation returns token(s)
     * else empty string
     * @param accountActivationDto
     * @return
     */
    @PostMapping("/registration/activation")
    public ResponseEntity activateAccountByCode(@Valid @RequestBody AccountActivationDto accountActivationDto) {
        try{
            TokenResponseDto tokens = userService.activateAccountByCode(accountActivationDto.getEmailMobile(), accountActivationDto.getCode(),
                    accountActivationDto.getClientId(),accountActivationDto.getClientSecret());
            return ResponseEntity.status(HttpStatus.OK_200).body(tokens == null ? "" : tokens);
        }catch (BusinessLogicException e){
            if(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH.getCode().equals(e.getErrorCode())){
                userService.updateAccountActivationTryCount(accountActivationDto);
            }
            throw e;
        }
    }

    /**
     * Called by user after clicking/tapping on activation link
     * Activates user account and verifies the contact info(email or mobile)
     * returns success/fail page
     * @param linkParam
     * @return
     */
    @GetMapping("/registration/activation/link/{linkParam}")
    private ModelAndView activateAccountByLink(@NotEmpty @PathVariable String linkParam) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            UserMetaChange umc = userService.activateAccountByLink(linkParam);
            modelAndView.setViewName("genericResult.html");//sucess
            modelAndView.addAllObjects(ResultPageDto.builder()
                    .success(true)
                    .title("Congratulations!")
                    .message1("You can now log in to your account with your "+ umc.getContactType().toString().toLowerCase())
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
