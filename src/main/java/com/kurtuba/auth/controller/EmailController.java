package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.model.EmailDetails;
import com.kurtuba.auth.service.EmailService;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/mail")
    public String
    sendMail(@RequestBody EmailDetails details) {
        String status = null;
        status = emailService.sendSimpleMail(details);
        return status;
    }

    @PostMapping("/mail/verification-code")
    public ResponseEntity<String> sendEmailVerificationMail(@RequestParam @NotEmpty String recipient, @RequestParam @NotEmpty String verificationCode) {

        emailService.sendValidationCodeMail(recipient, verificationCode);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Success");
    }

    @PostMapping("/mail/with-attachment")
    public String sendMailWithAttachment(
            @RequestBody EmailDetails details) {
        String status
                = emailService.sendMailWithAttachment(details);

        return status;
    }
}