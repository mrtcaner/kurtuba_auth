package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.utils.annotation.MobileNumber;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileVerificationRequestDto {

    @MobileNumber
    String mobile;
}
