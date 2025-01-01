package com.kurtuba.auth.data.model;

import com.kurtuba.auth.data.enums.EmailJobStateType;
import com.kurtuba.auth.data.enums.MailType;
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
@Table(name = "email_job")
public class EmailJob {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotEmpty
    String sender;

    @NotEmpty
    String recipient;

    @NotEmpty
    String subject;

    @NotEmpty
    @Column(length=10485760)
    String message;

    @NotNull
    @Enumerated(EnumType.STRING)
    MailType mailType;

    @NotNull
    LocalDateTime sendAfterDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    EmailJobStateType state;

    String error;

    int tryCount;

    int maxTryCount;

    @NotNull
    LocalDateTime createdDate;

    LocalDateTime updatedDate;
}
