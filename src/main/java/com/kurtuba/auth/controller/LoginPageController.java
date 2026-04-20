package com.kurtuba.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginPageController {

    @GetMapping("/auth/login")
    public String loginPage() {
        return "login";
    }
}
