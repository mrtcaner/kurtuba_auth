package com.kurtuba.auth.data.model;


import com.kurtuba.auth.data.enums.MetaChangeType;
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
@Table(name = "user_meta_change")
public class UserMetaChange {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotEmpty
    private String userId;

    //email address, phone number. In case of password
    private String meta;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MetaChangeType metaChangeType;

    private String code;

    private boolean executed;

    private LocalDateTime expirationDate;

    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;
}
