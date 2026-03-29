package com.kurtuba.auth.config;

import com.kurtuba.auth.data.repository.LocalizationAvailableLocaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequiredLocalizationConfigValidator implements ApplicationRunner {

    static final String FALLBACK_LANGUAGE_CODE = "en";
    static final String FALLBACK_COUNTRY_CODE = "us";

    private final LocalizationAvailableLocaleRepository localizationAvailableLocaleRepository;

    @Override
    public void run(ApplicationArguments args) {
        localizationAvailableLocaleRepository
                .findByLanguageCodeAndCountryCode(FALLBACK_LANGUAGE_CODE, FALLBACK_COUNTRY_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "Required localization config is missing: fallback locale en/us must exist."));
    }
}
