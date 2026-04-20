package com.kurtuba.auth.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRoleDto {

    private String id;
    private String userId;
    private RoleDto role;
    Instant createdDate;
}
