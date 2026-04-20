package com.kurtuba.auth.data.model;

import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.GenderType;
import com.kurtuba.auth.utils.annotation.EmailAddress;
import com.kurtuba.auth.utils.annotation.MobileNumber;
import com.kurtuba.auth.utils.annotation.UserName;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    @Size(min = 1, max = 100, message = "Name must be between 1-100 characters.")
    private String name;

    @Nullable//Can be null for different providers but not for kurtuba
    private String surname;

    @Nullable//can be created any time
    @UserName(notBlank = false)
    private String username;

    @Nullable
    private Instant birthdate;

    @Nullable
    @MobileNumber(notBlank = false)
    private String mobile;

    @Nullable
    @Column(nullable = true)
    @EmailAddress(notBlank = false)
    private String email;

    @NotBlank
    private String password;

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "user", cascade = CascadeType.ALL)
    private UserSetting userSetting;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
    private List<UserRole> userRoles;

    private boolean activated;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthProviderType authProvider;

    @Enumerated(EnumType.STRING)
    private GenderType gender;

    private boolean locked;

    private boolean blocked;

    private int failedLoginCount;

    private boolean showCaptcha;

    private boolean emailVerified;

    private boolean mobileVerified;

    private Instant lastLoginAttempt;

    @NotNull
    private Instant createdDate;

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
