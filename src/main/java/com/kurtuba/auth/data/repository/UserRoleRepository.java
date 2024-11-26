package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.UserRole;
import org.springframework.data.repository.CrudRepository;

public interface UserRoleRepository  extends CrudRepository<UserRole, Long> {
}
