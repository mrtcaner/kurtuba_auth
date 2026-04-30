package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.SMSRequestDto;
import com.kurtuba.auth.data.dto.SMSVerificationCheckDto;
import com.kurtuba.auth.data.dto.SMSVerificationDeleteDto;
import com.kurtuba.auth.data.dto.SMSVerificationRequestDto;
import com.kurtuba.auth.service.ISMSService;
import com.twilio.rest.verify.v2.service.Verification;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;


/**
 * Convenience controller for testing purposes
 * works only in the dev profile
 */
@RestController
@RequestMapping("/auth/sms")
@Profile("dev")
public class SmsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsController.class);

    final
    ISMSService smsService;

    public SmsController(ISMSService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("")
    public ResponseEntity<Object> sendSMS(@Valid @RequestBody SMSRequestDto smsRequestDto) {
        return ResponseEntity.ok(smsService.sendSMS(smsRequestDto.getRecipient(), smsRequestDto.getSender(),
                smsRequestDto.getMessage()));
    }

    @PostMapping("/verification")
    public ResponseEntity<Verification> sendVerificationSMS(@Valid @RequestBody SMSVerificationRequestDto smsRequestDto) {
        return ResponseEntity.ok(smsService.sendVerificationSMS(smsRequestDto.getRecipient()));
    }

    @PutMapping("/verification")
    public ResponseEntity<Boolean> checkVerification(@Valid @RequestBody SMSVerificationCheckDto smsRequestDto) {
        return ResponseEntity.ok(smsService.checkVerification(smsRequestDto.getRecipient(), smsRequestDto.getCode()));
    }

    @DeleteMapping("/verification")
    public ResponseEntity<Object> deleteVerification(@Valid @RequestBody SMSVerificationDeleteDto smsRequestDto) {
        return ResponseEntity.ok(smsService.deleteVerification(smsRequestDto.getSid()));
    }

    // twillio webhook
    // currently twilio cannot serve using turkish operator numbers, so this is not properly implemented
    @PostMapping("/message-status")
    @ResponseBody
    public void handleOutgoingSMSStatusChange(HttpServletRequest request) {
        request.getParameterMap().forEach((key, values) ->
                LOGGER.info("Twilio message-status webhook param {}={}", key, Arrays.stream(values).reduce("", String::concat)));
        LOGGER.info("Completed handling Twilio message-status webhook");
    }

}
