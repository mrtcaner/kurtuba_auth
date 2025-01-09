package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.Role;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RoleRepository extends CrudRepository<Role, String> {

    Optional<Role> findByName(String name);
}
