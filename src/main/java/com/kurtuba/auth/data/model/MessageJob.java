package com.kurtuba.auth.data.model;

import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MessageJobStateType;
import com.kurtuba.auth.data.enums.MessageServiceProviderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "message_job")
public class MessageJob {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    String sender;

    @NotBlank
    String recipient;

    String subject;

    @NotBlank
    @Column(length = 10485760)
    String message;

    @NotNull
    @Enumerated(EnumType.STRING)
    ContactType contactType;

    @NotNull
    @Enumerated(EnumType.STRING)
    MessageJobStateType state;

    @NotNull
    LocalDateTime sendAfterDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    MessageServiceProviderType serviceProvider;

    String sid;

    String error;

    int tryCount;

    int maxTryCount;

    @NotNull
    LocalDateTime createdDate;

    LocalDateTime updatedDate;
}
