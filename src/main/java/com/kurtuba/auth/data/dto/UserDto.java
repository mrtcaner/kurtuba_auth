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

import java.time.LocalDateTime;
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
    private List<UserRole> userRoles;
    private UserSetting userSetting;
    private LocalDateTime birthdate;
    private GenderType gender;
    private AuthProviderType authProvider;
    private boolean activated;
    private boolean locked;
    private int failedLoginCount;
    private boolean showCaptcha;
    private boolean emailVerified;
    private boolean mobileVerified;
    private LocalDateTime lastLoginAttempt;
    private LocalDateTime createdDate;

    public User toUser() {
        return User.builder()
                .id(id)
                .name(name)
                .surname(surname)
                .email(email)
                .username(username)
                .userRoles(userRoles)
                .userSetting(userSetting)
                .birthdate(birthdate)
                .gender(gender)
                .authProvider(authProvider)
                .mobile(mobile)
                .activated(activated)
                .locked(locked)
                .failedLoginCount(failedLoginCount)
                .showCaptcha(showCaptcha)
                .emailVerified(emailVerified)
                .mobileVerified(mobileVerified)
                .lastLoginAttempt(lastLoginAttempt)
                .createdDate(createdDate)
                .build();
    }

    public static UserDto fromUser(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .username(user.getUsername())
                .userRoles(user.getUserRoles())
                .userSetting(user.getUserSetting())
                .birthdate(user.getBirthdate())
                .gender(user.getGender())
                .authProvider(user.getAuthProvider())
                .mobile(user.getMobile())
                .activated(user.isActivated())
                .locked(user.isLocked())
                .failedLoginCount(user.getFailedLoginCount())
                .showCaptcha(user.isShowCaptcha())
                .emailVerified(user.isEmailVerified())
                .mobileVerified(user.isMobileVerified())
                .lastLoginAttempt(user.getLastLoginAttempt())
                .createdDate(user.getCreatedDate())
                .build();
    }


}