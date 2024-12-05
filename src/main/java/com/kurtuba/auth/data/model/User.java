package com.kurtuba.auth.data.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotEmpty
    @Size(min = 2, max = 100, message = "Name must be between 2-100 characters.")
    private String name;

    @Nullable//Can be null for different providers but not for kurtuba
    private String surname;

    @NotEmpty
    @Pattern(regexp = "^(?=.{2,32}$)(?![_.])(?!.*[_.]{2})[a-z0-9._]+(?<![_.])$")
    private String username;

    @Nullable
    private LocalDateTime birthdate;

    @Nullable
    private String phone;

    @NotEmpty
    @Pattern(regexp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
            + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$")
    private String email;

    @NotEmpty
    private String password;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(referencedColumnName = "id", name = "user_id")
    private List<UserRole> userRoles;

    @Column
    String bio;

    @Column
    String profilePic;

    @Column
    String profileCover;

    boolean canChangeUsername;

    private boolean activated;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    private boolean locked;

    private int failedLoginCount;

    private boolean showCaptcha;

    @Nullable
    private String emailValidationCode;

    private boolean emailValidated;

    @Nullable
    private LocalDateTime lastLoginAttempt;

    @NotNull
    private LocalDateTime createdDate;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            return id.equals( ((User) obj).getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }


}
