package com.kurtuba.auth.data.model;

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
@Table(name = "localization_available_locale")
public class LocalizationAvailableLocale {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotEmpty
    private String languageCode;

    @NotEmpty
    private String countryCode;

    @NotNull
    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;
}
