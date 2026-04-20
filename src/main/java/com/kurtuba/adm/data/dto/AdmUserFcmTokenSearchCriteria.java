package com.kurtuba.adm.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmUserFcmTokenSearchCriteria {
    private String userId;
    private String userEmail;
    private String userMobile;
    private String userRole;
    private String firebaseInstallationId;
    private String fcmToken;
}
