package com.kurtuba.auth.data.mapper;

import com.kurtuba.auth.data.dto.UserSettingDto;
import com.kurtuba.auth.data.dto.UserSettingLocaleDto;
import com.kurtuba.auth.data.model.UserSetting;
import com.kurtuba.auth.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSettingMapper {

    private final UserRepository userRepository;

    public UserSettingDto mapToDTO(UserSetting userSetting) {
        return UserSettingDto.builder()
                             .id(userSetting.getId())
                             .bio(userSetting.getBio())
                             .userId(userSetting.getUser().getId())
                             .canChangeUsername(userSetting.isCanChangeUsername())
                             .countryCode(userSetting.getCountryCode())
                             .profileCover(userSetting.getProfileCover())
                             .languageCode(userSetting.getLanguageCode())
                             .locale(UserSettingLocaleDto.builder()
                                                         .id(userSetting.getCountryCode() + "_" +
                                                             userSetting.getLanguageCode())
                                                         .countryCode(userSetting.getCountryCode())
                                                         .languageCode(userSetting.getLanguageCode())
                                                         .createdDate(userSetting.getCreatedDate())
                                                         .build())
                             .profilePic(userSetting.getProfilePic())
                             .createdDate(userSetting.getCreatedDate())
                             .build();
    }

    public UserSetting mapToUserSetting(UserSettingDto userSetting) {
        return UserSetting.builder()
                          .id(userSetting.getId())
                          .user(userSetting.getUserId() == null ? null
                                                                : userRepository.getUserById(userSetting.getUserId())
                                                                                .get())
                          .profileCover(userSetting.getProfileCover())
                          .profilePic(userSetting.getProfilePic())
                          .languageCode(userSetting.getLanguageCode())
                          .createdDate(userSetting.getCreatedDate())
                          .bio(userSetting.getBio())
                          .canChangeUsername(userSetting.isCanChangeUsername())
                          .countryCode(userSetting.getCountryCode())
                          .build();
    }
}