package com.kurtuba.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;


// currently twilio cannot serve using turkish operator numbers so this is not properly implemented
@RestController
@RequestMapping("/message-status")
public class WebhookController {

    @PostMapping("")
    @ResponseBody
    public void handleOutgoingSMSStatusChange(HttpServletRequest request) {
        request.getParameterMap().keySet().forEach(key -> System.out.println(key + ": " +
                Arrays.stream(request.getParameterMap().get(key)).reduce((s, s2) -> s += s2).orElse("")));
        System.out.println("*** ----------------------------------------------------------------------------- ***");
    }
}