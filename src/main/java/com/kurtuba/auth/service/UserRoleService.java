package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.Role;
import com.kurtuba.auth.data.model.User;
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

    final
    UserService userService;

    public UserRoleService(UserRoleRepository userRoleRepository, RoleRepository roleRepository, UserService userService) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
    }

    @Transactional
    public UserRole create(UserRole userRole){
        Role role = roleRepository.findByName(userRole.getRole().getName()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.ROLE_INVALID));
        userRole.setRole(role);
        return userRoleRepository.save(userRole);
    }

    @Transactional
    public UserRole addRoleToUser(String userId, String roleName) {
        User user = userService.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        userRoleRepository.findByUserIdAndRoleName(userId, roleName).ifPresent(existing -> {
            throw new BusinessLogicException(ErrorEnum.INVALID_PARAMETER.getCode(), "Role already assigned");
        });

        return create(UserRole.builder()
                .user(user)
                .role(Role.builder().name(roleName).build())
                .createdDate(java.time.Instant.now())
                .build());
    }

    @Transactional
    public void removeRoleFromUser(String userId, String roleName) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleName(userId, roleName).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.ROLE_INVALID));
        userRoleRepository.delete(userRole);
    }

}
