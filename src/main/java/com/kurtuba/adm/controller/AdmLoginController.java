package com.kurtuba.adm.controller;

import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.LoginService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth/adm/login")
public class AdmLoginController {

    private static final String ADMIN_WEB_CLIENT_NAME = "adm-web-client";

    private final LoginService loginService;
    private final RegisteredClientRepository registeredClientRepository;

    public AdmLoginController(LoginService loginService, RegisteredClientRepository registeredClientRepository) {
        this.loginService = loginService;
        this.registeredClientRepository = registeredClientRepository;
    }

    @GetMapping
    public String loginPage() {
        return "adm/login";
    }

    @PostMapping
    public String login(@RequestParam("username") @NotBlank String username,
                        @RequestParam("password") @NotBlank String password,
                        jakarta.servlet.http.HttpServletResponse response,
                        RedirectAttributes redirectAttributes) {
        try {
            RegisteredClient client = registeredClientRepository.findByClientName(ADMIN_WEB_CLIENT_NAME)
                                                                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID));
            TokensResponseDto tokenDto = loginService.authenticateAdminAndGetTokens(
                    username,
                    password,
                    client.getClientId(),
                    null);
            ResponseCookie cookie = ResponseCookie.from("jwt", tokenDto.accessToken)
                    .httpOnly(client.isCookieHttpOnly())
                    .secure(client.isCookieSecure())
                    .path("/")
                    .maxAge(client.getCookieMaxAgeSeconds())
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            return "redirect:/auth/adm";
        } catch (BusinessLogicException exception) {
            redirectAttributes.addAttribute("error", "");
            return "redirect:/auth/adm/login";
        }
    }
}
