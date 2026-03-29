package com.kurtuba.auth.config;

import com.kurtuba.auth.data.model.LocalizationAvailableLocale;
import com.kurtuba.auth.data.repository.LocalizationAvailableLocaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequiredLocalizationConfigValidatorTest {

    @Mock
    private LocalizationAvailableLocaleRepository localizationAvailableLocaleRepository;

    @InjectMocks
    private RequiredLocalizationConfigValidator validator;

    @Test
    void run_allowsStartupWhenFallbackLocaleExists() {
        when(localizationAvailableLocaleRepository.findByLanguageCodeAndCountryCode("en", "us"))
                .thenReturn(Optional.of(LocalizationAvailableLocale.builder()
                        .id("locale-en-us")
                        .languageCode("en")
                        .countryCode("us")
                        .createdDate(Instant.now())
                        .build()));

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void run_failsStartupWhenFallbackLocaleIsMissing() {
        when(localizationAvailableLocaleRepository.findByLanguageCodeAndCountryCode("en", "us"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments(new String[0])));
    }
}
