package com.kurtuba.auth.data.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingDto {

    private String id;

    String userId;

    String bio;

    String profilePic;

    String profileCover;

    boolean canChangeUsername;

    UserSettingLocaleDto locale; // todo: backward compatibility object for mobile 1.0.0.Alpha-3 release

    private String languageCode;

    private String countryCode;

    private Instant createdDate;
}
