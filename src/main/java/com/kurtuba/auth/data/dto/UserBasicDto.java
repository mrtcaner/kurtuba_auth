package com.kurtuba.auth.data.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicDto {
    private String id;
    private String name;
    private String surname;
    private String email;
    private String mobile;
    private String username;
    private List<String> userRoles;
    private boolean emailVerified;
    private boolean mobileVerified;
    private Instant lastLoginAttempt;
    private Instant createdDate;
}
