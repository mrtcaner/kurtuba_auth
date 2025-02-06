package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.UserToken;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends CrudRepository<UserToken, String> {

    Optional<UserToken> findByJtiAndBlockedAndRefreshTokenExpAfter(String jti, boolean blocked, LocalDateTime exp);

    Optional<UserToken> findByJtiAndRefreshTokenExpAfter(String jti, LocalDateTime exp);

    List<UserToken> findAllByUserId(String userId);

    List<UserToken> findAllByUserIdAndBlocked(String userId, boolean blocked);

    Optional<UserToken> findByJti(String jti);

    Optional<UserToken> findByJtiAndBlocked(String jti, boolean blocked);

}
