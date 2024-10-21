package com.parafusion.auth.data.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_token")
public class UserToken {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotEmpty
    String userId;

    @NotEmpty
    String tokenHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    ClientType clientType;

    boolean active;

    @NotNull
    LocalDateTime createdDate;

    LocalDateTime updatedDate;


}
