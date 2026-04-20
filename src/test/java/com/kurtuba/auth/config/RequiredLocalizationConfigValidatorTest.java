package com.kurtuba.auth.config;

import com.kurtuba.auth.data.model.LocalizationSupportedCountry;
import com.kurtuba.auth.data.model.LocalizationSupportedLang;
import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
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
    private LocalizationSupportedLangRepository localizationSupportedLangRepository;

    @Mock
    private LocalizationSupportedCountryRepository localizationSupportedCountryRepository;

    @InjectMocks
    private RequiredLocalizationConfigValidator validator;

    @Test
    void run_allowsStartupWhenFallbackLocaleExists() {
        when(localizationSupportedLangRepository.findByLanguageCode("en"))
                .thenReturn(Optional.of(LocalizationSupportedLang.builder()
                        .id("lang-en")
                        .languageCode("en")
                        .createdDate(Instant.now())
                        .build()));
        when(localizationSupportedCountryRepository.findByCountryCode("us"))
                .thenReturn(Optional.of(LocalizationSupportedCountry.builder()
                        .id("country-us")
                        .countryCode("us")
                        .createdDate(Instant.now())
                        .build()));

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void run_failsStartupWhenFallbackLocaleIsMissing() {
        when(localizationSupportedLangRepository.findByLanguageCode("en"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments(new String[0])));
    }
}
