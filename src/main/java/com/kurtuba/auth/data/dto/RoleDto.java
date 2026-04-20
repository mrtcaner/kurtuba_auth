
package com.kurtuba.auth.data.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kurtuba.auth.data.model.Role;
import com.kurtuba.auth.data.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleDto {

    private String id;
    private String name;
}
