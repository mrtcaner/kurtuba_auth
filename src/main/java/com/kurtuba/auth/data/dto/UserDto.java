package com.kurtuba.auth.data.dto;


import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserRole;
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
    private String username;
    private List<UserRole> userRoles;
    private String bio;
    private String profilePicture;
    private String profileCover;
    private boolean canChangeUsername;
    private LocalDateTime birthdate;
    private AuthProviderType authProvider;
    private String mobile;
    private boolean activated;
    private boolean locked;
    private int failedLoginCount;
    private boolean showCaptcha;
    private boolean emailVerified;
    private boolean mobileVerified;
    private LocalDateTime lastLoginAttempt;
    private String language;
    private String country;
    private LocalDateTime createdDate;

    public User toUser() {
        return User.builder()
                .id(id)
                .name(name)
                .surname(surname)
                .email(email)
                .username(username)
                .userRoles(userRoles)
                .bio(bio)
                .profilePic(profilePicture)
                .profileCover(profileCover)
                .canChangeUsername(canChangeUsername)
                .birthdate(birthdate)
                .authProvider(authProvider)
                .mobile(mobile)
                .activated(activated)
                .locked(locked)
                .failedLoginCount(failedLoginCount)
                .showCaptcha(showCaptcha)
                .emailVerified(emailVerified)
                .mobileVerified(mobileVerified)
                .lastLoginAttempt(lastLoginAttempt)
                .language(language)
                .country(country)
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
                .bio(user.getBio())
                .profilePicture(user.getProfilePic())
                .profileCover(user.getProfileCover())
                .canChangeUsername(user.isCanChangeUsername())
                .birthdate(user.getBirthdate())
                .authProvider(user.getAuthProvider())
                .mobile(user.getMobile())
                .activated(user.isActivated())
                .locked(user.isLocked())
                .failedLoginCount(user.getFailedLoginCount())
                .showCaptcha(user.isShowCaptcha())
                .emailVerified(user.isEmailVerified())
                .mobileVerified(user.isMobileVerified())
                .lastLoginAttempt(user.getLastLoginAttempt())
                .language(user.getLanguage())
                .country(user.getCountry())
                .createdDate(user.getCreatedDate())
                .build();
    }


}