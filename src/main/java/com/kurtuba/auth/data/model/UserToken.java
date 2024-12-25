package com.kurtuba.auth.data.model;

import com.kurtuba.auth.utils.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private String userId;

    @NotEmpty
    private String refreshToken;

    @NotNull
    private LocalDateTime refreshTokenExp;

    @NotEmpty
    private String jti;

    @NotEmpty
    private String clientId;

    @NotEmpty
    @Convert(converter = StringListConverter.class)
    private List<String> aud;

    @Convert(converter = StringListConverter.class)
    private List<String> scopes;

    private boolean blocked;

    @NotNull
    private LocalDateTime createdDate;

    @NotNull
    private LocalDateTime expirationDate;


}
