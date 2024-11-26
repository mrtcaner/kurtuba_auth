package com.kurtuba.auth.data.model.dto;


import com.kurtuba.auth.data.model.AuthProvider;
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
    private AuthProvider authProvider;
    private String phone;
    private boolean activated;
    private boolean locked;
    private int failedLoginCount;
    private boolean showCaptcha;
    private String emailValidationCode;
    private boolean emailValidated;
    private LocalDateTime lastLoginAttempt;
    private LocalDateTime dateCreated;

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
                .phone(phone)
                .activated(activated)
                .locked(locked)
                .failedLoginCount(failedLoginCount)
                .showCaptcha(showCaptcha)
                .emailValidationCode(emailValidationCode)
                .emailValidated(emailValidated)
                .lastLoginAttempt(lastLoginAttempt)
                .createdDate(dateCreated)
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
                .phone(user.getPhone())
                .activated(user.isActivated())
                .locked(user.isLocked())
                .failedLoginCount(user.getFailedLoginCount())
                .showCaptcha(user.isShowCaptcha())
                .emailValidationCode(user.getEmailValidationCode())
                .emailValidated(user.isEmailValidated())
                .lastLoginAttempt(user.getLastLoginAttempt())
                .dateCreated(user.getCreatedDate())
                .build();
    }


}