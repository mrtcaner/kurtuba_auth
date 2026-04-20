package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.utils.annotation.EmailMobile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestDto {

    @EmailMobile
    String emailMobile;

    boolean byCode;

}
