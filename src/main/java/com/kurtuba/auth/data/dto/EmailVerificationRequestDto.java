package com.kurtuba.auth.data.dto;

import com.kurtuba.auth.utils.annotation.EmailAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequestDto {

    @EmailAddress
    String email;

    boolean byCode;
}
