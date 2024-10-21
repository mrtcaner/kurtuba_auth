package com.parafusion.auth.data.repository;

import com.parafusion.auth.data.model.ClientType;
import com.parafusion.auth.data.model.UserToken;
import org.springframework.data.repository.CrudRepository;

public interface UserTokenRepository extends CrudRepository<UserToken, Long> {

	UserToken getUserTokenByUserIdAndClientTypeAndActive(String email, ClientType clientType, boolean active);

}
