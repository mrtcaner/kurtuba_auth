package com.kurtuba.auth.data.model;

import com.kurtuba.auth.data.enums.AuthoritiesType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_role")
public class UserRole implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotEmpty
    @Column(name = "user_id")
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthoritiesType role;

    @NotNull
    LocalDateTime createdDate;
}
