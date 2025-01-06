package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, String> {

	@Query("SELECT u FROM User u WHERE u.username = :username")
	Optional<User> getUserByUsername(@Param("username") String username);
	User getUsersByEmail(String email);
	@Query("SELECT u FROM User u WHERE u.email = :emailUsername or u.username = :emailUsername")
	Optional<User> getUserByEmailOrUsername(String emailUsername);

	@Query("SELECT u FROM User u WHERE u.email = :emailMobile or u.mobile = :emailMobile")
	Optional<User> getUserByEmailOrMobile(String emailMobile);

	Optional<User> getUserById(String id);

	Optional<User> getUserByEmail(String email);

	Optional<User> getUserByMobile(String mobile);

	Optional<User> getUserByEmailAndAuthProvider(String email, AuthProviderType provider);

	Optional<User> getUserByEmailAndActivatedIsFalse(String email);

	Optional<User> getUserByMobileAndActivatedIsFalse(String email);

}
