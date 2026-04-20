package com.kurtuba.auth.service;

import com.kurtuba.auth.data.repository.UserFcmTokenRepository;
import com.kurtuba.auth.data.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final UserTokenRepository userTokenRepository;

    @Transactional
    public void doLogout(String jti) {
        userTokenRepository.findByJti(jti).ifPresent(userToken -> {
            userToken.setBlocked(true);
            userToken.setUpdatedDate(Instant.now());
            userTokenRepository.save(userToken);
        });
    }


    /**
     * Delete all fcm tokens for the given user and firebaseInstallationId
     * @param userId
     * @param firebaseInstallationId
     */
    @Transactional
    public void doLogoutFcm(String userId, String firebaseInstallationId) {
        userFcmTokenRepository.deleteByUserIdAndFirebaseInstallationId(userId, firebaseInstallationId);
    }
}
