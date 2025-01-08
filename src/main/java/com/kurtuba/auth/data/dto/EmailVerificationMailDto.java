package com.kurtuba.auth.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class EmailVerificationMailDto {

    String title;
    String greet;
    String verificationCode;
    String displayCode;
    String verificationLink;
    String verifyEmailBtnLabel;
    String displayLink;
    String epilogue;
    String prologue;
    String closing;
    String closingSubject;
    String getInTouch;
}
