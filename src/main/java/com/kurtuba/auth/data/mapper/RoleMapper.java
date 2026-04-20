package com.kurtuba.auth.data.mapper;

import com.kurtuba.auth.data.dto.RoleDto;
import com.kurtuba.auth.data.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleMapper {

    public RoleDto mapToDTO(Role role) {
        return RoleDto.builder().id(role.getId()).name(role.getName()).build();
    }


    public Role mapToRole(RoleDto roleDto) {
        return Role.builder().id(roleDto.getId()).name(roleDto.getName()).build();
    }


}
