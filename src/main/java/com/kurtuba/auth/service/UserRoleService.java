package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.Role;
import com.kurtuba.auth.data.model.UserRole;
import com.kurtuba.auth.data.repository.RoleRepository;
import com.kurtuba.auth.data.repository.UserRoleRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleService {

    final
    UserRoleRepository userRoleRepository;

    final
    RoleRepository roleRepository;

    public UserRoleService(UserRoleRepository userRoleRepository, RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public UserRole create(UserRole userRole){
        Role role = roleRepository.findByName(userRole.getRole().getName()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.ROLE_INVALID));
        userRole.setRole(role);
        return userRoleRepository.save(userRole);
    }

}
