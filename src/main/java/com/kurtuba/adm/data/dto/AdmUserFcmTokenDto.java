package com.kurtuba.adm.data.dto;

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
public class AdmUserFcmTokenDto {
    private String userId;
    private String userEmail;
    private String userMobile;
    private List<String> userRoles;
    private String registeredClientId;
    private String fcmToken;
    private String firebaseInstallationId;
    private Instant updatedAt;
}
