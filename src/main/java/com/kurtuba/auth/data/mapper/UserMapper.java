package com.kurtuba.auth.data.mapper;

import com.kurtuba.adm.data.dto.AdmUserDto;
import com.kurtuba.auth.data.dto.RegistrationDto;
import com.kurtuba.auth.data.dto.UserBasicDto;
import com.kurtuba.auth.data.dto.UserDto;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final UserSettingMapper userSettingMapper;
    private final UserRoleMapper userRoleMapper;

    public User maptoUser(RegistrationDto registrationDto) {
        String normalizedEmail = StringUtils.hasLength(registrationDto.getEmail()) ? registrationDto.getEmail() : null;
        String normalizedMobile = StringUtils.hasLength(registrationDto.getMobile()) ? registrationDto.getMobile() : null;
        String normalizedUsername = StringUtils.hasLength(registrationDto.getUsername()) ? registrationDto.getUsername() : null;

        return User.builder()
                   .name(registrationDto.getName())
                   .surname(registrationDto.getSurname())
                   .username(normalizedUsername)
                   .email(normalizedEmail)
                   .password(registrationDto.getPassword())
                   .authProvider(registrationDto.getAuthProvider())
                   .mobile(normalizedMobile)
                   .userSetting(UserSetting.builder()
                                           .canChangeUsername(false)
                                           .languageCode(registrationDto.getLanguageCode())
                                           .countryCode(registrationDto.getCountryCode())
                                           .build())
                   .activated(false)
                   .locked(false)
                   .failedLoginCount(0)
                   .showCaptcha(false)
                   .emailVerified(false)
                   .mobileVerified(false)
                   .lastLoginAttempt(Instant.now())
                   .createdDate(Instant.now())
                   .build();
    }

    public UserDto mapToUserDto(User user) {
        return UserDto.builder()
                      .id(user.getId())
                      .name(user.getName())
                      .surname(user.getSurname())
                      .email(user.getEmail())
                      .username(user.getUsername())
                      .userRoles(user.getUserRoles().stream().map(userRoleMapper::mapToDTO).toList())
                      .userSetting(userSettingMapper.mapToDTO(user.getUserSetting()))
                      .birthdate(user.getBirthdate())
                      .gender(user.getGender())
                      .authProvider(user.getAuthProvider())
                      .mobile(user.getMobile())
                      .activated(user.isActivated())
                      .locked(user.isLocked())
                      .blocked(user.isBlocked())
                      .failedLoginCount(user.getFailedLoginCount())
                      .showCaptcha(user.isShowCaptcha())
                      .emailVerified(user.isEmailVerified())
                      .mobileVerified(user.isMobileVerified())
                      .lastLoginAttempt(user.getLastLoginAttempt())
                      .createdDate(user.getCreatedDate())
                      .build();
    }

    public AdmUserDto mapToAdmUserDto(User user, Instant lastTokenCreatedDate) {
        return AdmUserDto.builder()
                         .id(user.getId())
                         .name(user.getName())
                         .surname(user.getSurname())
                         .email(user.getEmail())
                         .mobile(user.getMobile())
                         .username(user.getUsername())
                         .userRoles(user.getUserRoles())
                         .userSetting(user.getUserSetting() == null ? null
                                                                    : userSettingMapper.mapToDTO(user.getUserSetting()))
                         .authProvider(user.getAuthProvider())
                         .activated(user.isActivated())
                         .locked(user.isLocked())
                         .blocked(user.isBlocked())
                         .failedLoginCount(user.getFailedLoginCount())
                         .showCaptcha(user.isShowCaptcha())
                         .emailVerified(user.isEmailVerified())
                         .mobileVerified(user.isMobileVerified())
                         .lastLoginAttempt(user.getLastLoginAttempt())
                         .createdDate(user.getCreatedDate())
                         .lastTokenCreatedDate(lastTokenCreatedDate)
                         .build();
    }

    public UserBasicDto mapToUserBasicDto(User user) {
        return UserBasicDto.builder()
                           .id(user.getId())
                           .username(user.getUsername())
                           .email(user.getEmail())
                           .mobile(user.getMobile())
                           .emailVerified(user.isEmailVerified())
                           .mobileVerified(user.isMobileVerified())
                           .userRoles(
                                   user.getUserRoles().stream().map(userRole -> userRole.getRole().getName()).toList())
                           .name(user.getName())
                           .surname(user.getSurname())
                           .lastLoginAttempt(user.getLastLoginAttempt())
                           .createdDate(user.getCreatedDate())
                           .build();
    }
}
