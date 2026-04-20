package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.GenderType;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserRole;
import com.kurtuba.auth.data.model.UserSetting;
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
public class UserDto {
    private String id;
    private String name;
    private String surname;
    private String email;
    private String mobile;
    private String username;
    private List<UserRoleDto> userRoles;
    private UserSettingDto userSetting;
    private Instant birthdate;
    private GenderType gender;
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
}
