package com.kurtuba.auth.data.model;

import com.kurtuba.auth.utils.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_token")
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String userId;

    @NotBlank
    private String refreshToken;

    @NotNull
    private Instant refreshTokenExp;

    @Column(name = "refresh_token_used", nullable = false, columnDefinition = "boolean default false")
    private boolean refreshTokenUsed;

    @NotBlank
    private String jti;

    @NotBlank
    private String clientId;

    @NotEmpty
    @Convert(converter = StringListConverter.class)
    private List<String> auds;

    @Convert(converter = StringListConverter.class)
    private List<String> scopes;

    //todo: introduce enum(blocked, revoked, force-logout etc.)
    private boolean blocked;

    @NotNull
    private Instant createdDate;

    private Instant updatedDate;

    private Instant usedDate;

    @NotNull
    private Instant expirationDate;


}
