package com.kurtuba.adm.data.dto;

import com.kurtuba.auth.data.dto.UserSettingDto;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.model.UserRole;
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
public class AdmUserDto {
    private String id;
    private String name;
    private String surname;
    private String email;
    private String mobile;
    private String username;
    private List<UserRole> userRoles;
    private UserSettingDto userSetting;
    private AuthProviderType authProvider;
    private boolean activated;
    private boolean locked;
    private boolean blocked;
    private int failedLoginCount;
    private boolean showCaptcha;
    private boolean emailVerified;
    private boolean mobileVerified;
    private Instant lastLoginAttempt;
    private Instant createdDate;
    private Instant lastTokenCreatedDate;
}
