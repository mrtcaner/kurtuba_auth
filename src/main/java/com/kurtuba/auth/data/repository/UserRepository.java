package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends CrudRepository<User, String> {

	@Query("SELECT u FROM User u WHERE u.username = :username")
	User getUserByUsername(@Param("username") String username);
	User getUsersByEmail(String email);
	@Query("SELECT u FROM User u WHERE u.email = :emailUsername or u.username = :emailUsername")
	User getUserByEmailOrUsername(String emailUsername);

	@Query("SELECT u FROM User u WHERE u.email = :emailMobile or u.mobile = :emailMobile")
	User getUserByEmailOrMobile(String emailMobile);

	User getUserById(String id);

	User getUserByEmail(String email);

	User getUserByMobile(String mobile);

	User getUserByEmailAndAuthProvider(String email, AuthProviderType provider);

	User getUserByEmailAndActivatedIsFalse(String email);

	User getUserByMobileAndActivatedIsFalse(String email);

}
