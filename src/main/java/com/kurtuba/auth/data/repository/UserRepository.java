package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.AuthProvider;
import com.kurtuba.auth.data.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends CrudRepository<User, Long> {

	@Query("SELECT u FROM User u WHERE u.email = :username")
	User getUserByUsername(@Param("username") String username);
	User getUsersByEmail(String email);
	@Query("SELECT u FROM User u WHERE u.email = :emailUsername or u.username = :emailUsername")
	User getUserByEmailOrUsername(String emailUsername);

	User getUserById(String id);

	User getUserByEmail(String email);

	User getUserByEmailAndAuthProvider(String email, AuthProvider provider);

	User getUserByEmailAndEmailValidatedIsFalseAndEmailValidationCodeIsNotNull(String email);

}
