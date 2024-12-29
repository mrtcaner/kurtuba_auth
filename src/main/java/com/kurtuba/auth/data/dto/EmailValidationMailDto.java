package com.kurtuba.auth.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class EmailValidationMailDto {

    String title;
    String greet;
    String validationCode;
    String displayCode;
    String validationLink;
    String displayLink;
    String msg1;
    String msg2;
}
