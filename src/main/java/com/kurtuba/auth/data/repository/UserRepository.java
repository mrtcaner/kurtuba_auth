package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

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

	@Query("SELECT u FROM User u WHERE u.username IS NULL OR u.username = ''")
	List<User> findUsersWithoutUsername();

    @Query("""
                SELECT u FROM User u
                            inner join UserRole ur on ur.user.id = u.id
                            inner join Role r on r.id = ur.role.id
                WHERE u.activated = true and u.blocked = false and r.name = 'ADMIN'
            """)
	List<User> getAdminUsers();
}
