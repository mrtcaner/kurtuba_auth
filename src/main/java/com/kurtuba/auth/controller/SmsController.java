package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.SMSRequestDto;
import com.kurtuba.auth.data.dto.SMSVerificationCheckDto;
import com.kurtuba.auth.data.dto.SMSVerificationDeleteDto;
import com.kurtuba.auth.data.dto.SMSVerificationRequestDto;
import com.kurtuba.auth.service.ISMSService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("sms")
@Profile("dev")
public class SmsController {

    final
    ISMSService smsService;

    public SmsController(ISMSService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("")
    public ResponseEntity sendSMS(@Valid @RequestBody SMSRequestDto smsRequestDto) {
        return ResponseEntity.ok(smsService.sendSMS(smsRequestDto.getRecipient(), smsRequestDto.getSender(),
                smsRequestDto.getMessage()));
    }

    @PostMapping("/verification")
    public ResponseEntity sendVerificationSMS(@Valid @RequestBody SMSVerificationRequestDto smsRequestDto) {
        return ResponseEntity.ok(smsService.sendVerificationSMS(smsRequestDto.getRecipient()));
    }

    @PutMapping("/verification")
    public ResponseEntity checkVerification(@Valid @RequestBody SMSVerificationCheckDto smsRequestDto) {
        return ResponseEntity.ok(smsService.checkVerification(smsRequestDto.getRecipient(), smsRequestDto.getCode()));
    }

    @DeleteMapping("/verification")
    public ResponseEntity deleteVerification(@Valid @RequestBody SMSVerificationDeleteDto smsRequestDto) {
        return ResponseEntity.ok(smsService.deleteVerification(smsRequestDto.getSid()));
    }


}


