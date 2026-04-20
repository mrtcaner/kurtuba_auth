package com.kurtuba.auth.data.mapper;

import com.kurtuba.auth.data.dto.UserRoleDto;
import com.kurtuba.auth.data.model.UserRole;
import com.kurtuba.auth.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRoleMapper {

    private final RoleMapper roleMapper;
    private final UserRepository userRepository;

    public UserRoleDto mapToDTO(UserRole userRole) {
        return UserRoleDto.builder()
                   .id(userRole.getId())
                   .userId(userRole.getUser().getId())
                   .role(roleMapper.mapToDTO(userRole.getRole()))
                   .createdDate(userRole.getCreatedDate())
                   .build();
    }

    public UserRole mapToUserRole(UserRoleDto userRoleDto) {
        return UserRole.builder()
                .id(userRoleDto.getId())
                .user(userRoleDto.getUserId() != null ? userRepository.getUserById(userRoleDto.getUserId()).get() :
                      null)
                .role(roleMapper.mapToRole(userRoleDto.getRole()))
                .createdDate(userRoleDto.getCreatedDate())
                .build();
    }

}
