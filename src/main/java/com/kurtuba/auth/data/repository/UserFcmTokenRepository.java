package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.UserFcmToken;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, String> {

    interface AdminUserFcmTokenProjection {
        String getUserId();
        String getUserEmail();
        String getUserMobile();
        String getRegisteredClientId();
        String getFcmToken();
        String getFirebaseInstallationId();
        java.time.Instant getUpdatedAt();
    }

    List<UserFcmToken> findByUserId(String userId);

    List<UserFcmToken> findByUserIdIn(Collection<String> userIds);

    void deleteByUserIdAndFirebaseInstallationId(String userId, String firebaseInstallationId);

    Optional<UserFcmToken> findByFcmToken(String fcmToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000")})
    @Query("SELECT u FROM UserFcmToken u WHERE u.firebaseInstallationId = :installationId")
    Optional<UserFcmToken> findByInstallationIdForUpdate(String installationId);

    int deleteByFcmTokenIn(List<String> fcmTokens);

    @Query("""
            select f.userId as userId,
                   u.email as userEmail,
                   u.mobile as userMobile,
                   f.registeredClientId as registeredClientId,
                   f.fcmToken as fcmToken,
                   f.firebaseInstallationId as firebaseInstallationId,
                   f.updatedAt as updatedAt
            from UserFcmToken f
            left join User u on u.id = f.userId
            order by f.updatedAt desc, f.userId asc
            """)
    List<AdminUserFcmTokenProjection> findAllForAdminList();

    @Query("""
            select f.userId as userId,
                   u.email as userEmail,
                   u.mobile as userMobile,
                   f.registeredClientId as registeredClientId,
                   f.fcmToken as fcmToken,
                   f.firebaseInstallationId as firebaseInstallationId,
                   f.updatedAt as updatedAt
            from UserFcmToken f
            left join User u on u.id = f.userId
            where (:userId = '' or lower(f.userId) like concat('%', :userId, '%'))
              and (:userEmail = '' or lower(u.email) like concat('%', :userEmail, '%'))
              and (:userMobile = '' or lower(u.mobile) like concat('%', :userMobile, '%'))
              and (:firebaseInstallationId = '' or lower(f.firebaseInstallationId) like concat('%', :firebaseInstallationId, '%'))
              and (:fcmToken = '' or lower(f.fcmToken) like concat('%', :fcmToken, '%'))
              and (:userRole = '' or exists (
                    select 1
                    from UserRole ur
                    join ur.role r
                    where ur.user.id = f.userId
                      and lower(r.name) like concat('%', :userRole, '%')
              ))
            order by f.updatedAt desc, f.userId asc
            """)
    List<AdminUserFcmTokenProjection> searchForAdminList(@Param("userId") String userId,
                                                         @Param("userEmail") String userEmail,
                                                         @Param("userMobile") String userMobile,
                                                         @Param("userRole") String userRole,
                                                         @Param("firebaseInstallationId") String firebaseInstallationId,
                                                         @Param("fcmToken") String fcmToken);
}
