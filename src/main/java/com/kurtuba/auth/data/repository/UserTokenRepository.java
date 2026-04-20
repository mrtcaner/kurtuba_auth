package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.UserToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends CrudRepository<UserToken, String> {

    interface UserLatestTokenCreatedDateProjection {
        String getUserId();
        Instant getLastTokenCreatedDate();
    }

    List<UserToken> findAllByUserId(String userId);

    List<UserToken> findAllByUserIdAndBlocked(String userId, boolean blocked);

    @Query("""
            select ut.userId as userId, max(ut.createdDate) as lastTokenCreatedDate
            from UserToken ut
            where ut.userId in :userIds
            group by ut.userId
            """)
    List<UserLatestTokenCreatedDateProjection> findLatestCreatedDatesByUserIds(@Param("userIds") Collection<String> userIds);

    Optional<UserToken> findByJti(String jti);

    Optional<UserToken> findByJtiAndBlocked(String jti, boolean blocked);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update UserToken ut
            set ut.refreshTokenUsed = true, ut.usedDate = :now
            where ut.id = :tokenId
              and ut.refreshTokenUsed = false
              and ut.blocked = false
              and ut.refreshTokenExp is not null
              and ut.refreshTokenExp >= :now
            """)
    int markRefreshTokenAsUsedIfAvailable(@Param("tokenId") String tokenId, @Param("now") Instant now);

}
