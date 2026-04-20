package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.UserRole;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRoleRepository  extends CrudRepository<UserRole, String> {

    Optional<UserRole> findByUserIdAndRoleName(String userId, String roleName);
}
