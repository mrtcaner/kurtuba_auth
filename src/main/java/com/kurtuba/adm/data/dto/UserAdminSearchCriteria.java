package com.kurtuba.adm.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminSearchCriteria {
    private String id;
    private String username;
    private String email;
    private String mobile;
    private String name;
    private String surname;
    private String authProvider;
    private String locale;
    private String role;
    private String activated;
    private String locked;
    private String blocked;
    private String showCaptcha;
    private String emailVerified;
    private String mobileVerified;
}
