package com.kurtuba.auth.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_setting")
public class UserSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ToString.Exclude
    @JsonIgnore
    private User user;

    String bio;

    String profilePic;

    String profileCover;

    boolean canChangeUsername;

    @NotBlank
    private String languageCode;

    @NotBlank
    private String countryCode;

    @NotNull
    private Instant createdDate;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserSetting) {
            return id.equals(((UserSetting) obj).getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}
