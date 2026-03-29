package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.LocalizationAvailableLocale;
import org.springframework.data.repository.CrudRepository;


import java.util.List;
import java.util.Optional;

public interface LocalizationAvailableLocaleRepository extends CrudRepository<LocalizationAvailableLocale, String> {

    Optional<LocalizationAvailableLocale> findByLanguageCodeAndCountryCode(String language, String countryCode);

    List<LocalizationAvailableLocale> findAllByOrderByLanguageCodeAscCountryCodeAsc();

    List<LocalizationAvailableLocale> findByLanguageCode(String languageCode);

    List<LocalizationAvailableLocale> findByCountryCode(String languageCode);

}
