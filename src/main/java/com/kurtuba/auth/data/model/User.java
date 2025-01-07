package com.kurtuba.auth.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.utils.annotation.EmailAddress;
import com.kurtuba.auth.utils.annotation.MobileNumber;
import com.kurtuba.auth.utils.annotation.UserName;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    @Nullable//can be created any time
    @UserName(notEmpty = false)
    private String username;

    @Nullable
    private LocalDateTime birthdate;

    @Nullable
    @MobileNumber(notEmpty = false)
    private String mobile;

    @NotEmpty
    @EmailAddress(notEmpty = false)
    private String email;

    @NotEmpty
    private String password;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(referencedColumnName = "id", name = "user_id")
    @JsonIgnore
    private List<UserRole> userRoles;

    String bio;

    String profilePic;

    String profileCover;

    boolean canChangeUsername;

    private boolean activated;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthProviderType authProvider;

    private boolean locked;

    private int failedLoginCount;

    private boolean showCaptcha;

    private boolean emailVerified;

    private boolean mobileVerified;

    @Nullable
    private LocalDateTime lastLoginAttempt;

    @NotEmpty
    private String language;

    @NotEmpty
    private String country;

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
