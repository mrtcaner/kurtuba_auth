package com.kurtuba.auth.config;

import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequiredLocalizationConfigValidator implements ApplicationRunner {

    static final String FALLBACK_LANGUAGE_CODE = "en";
    static final String FALLBACK_COUNTRY_CODE = "us";

    private final LocalizationSupportedLangRepository localizationSupportedLangRepository;
    private final LocalizationSupportedCountryRepository localizationSupportedCountryRepository;

    @Override
    public void run(ApplicationArguments args) {
        localizationSupportedLangRepository
                .findByLanguageCode(FALLBACK_LANGUAGE_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "Required localization config is missing: fallback language en must exist."));
        localizationSupportedCountryRepository
                .findByCountryCode(FALLBACK_COUNTRY_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "Required localization config is missing: fallback country us must exist."));
    }
}
