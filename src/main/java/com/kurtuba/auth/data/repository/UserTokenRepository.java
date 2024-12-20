package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.UserToken;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;

public interface UserTokenRepository extends CrudRepository<UserToken, String> {

    UserToken findByJtiAndBlockedAndRefreshTokenExpAfter(String jti, boolean blocked, LocalDateTime exp);

}
