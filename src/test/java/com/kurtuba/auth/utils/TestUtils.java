package com.kurtuba.auth.utils;

import com.kurtuba.auth.data.dto.RegistrationDto;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.model.Role;
import com.kurtuba.auth.data.model.UserRole;
import com.kurtuba.auth.data.model.UserSetting;

public class TestUtils {


    public static UserSetting defaultUserSettingBuilder(){
        return UserSetting.builder()
                .canChangeUsername(false)
                .user(null)
                .languageCode("en")
                .countryCode("tr")
                .bio("")
                .profileCover("")
                .profilePic("")
                .id("1").build();
    }

    public static UserRole defaultUserRoleBuilder(){
        return UserRole.builder()
                .user(null)
                .role(Role.builder()
                        .name(AuthoritiesType.USER.name())
                        .id("1")
                        .build())
                .build();
    }


    public static RegistrationDto defaultRegistrationDtoBuilder(){
        return RegistrationDto.builder()
                .name("aa")
                .surname("bb")
                .username("cc")
                .email("user12345@user.com")
                .mobile("+905122345678")
                .password("a.123456")
                .authProvider(AuthProviderType.KURTUBA)
                .preferredVerificationContact(ContactType.EMAIL)
                .verificationByCode(true)
                .languageCode("en")
                .countryCode("tr")
                .build();
    }

}
