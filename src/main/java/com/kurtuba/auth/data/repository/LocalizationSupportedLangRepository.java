package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.LocalizationSupportedLang;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface LocalizationSupportedLangRepository extends CrudRepository<LocalizationSupportedLang, String> {

    Optional<LocalizationSupportedLang> findByLanguageCode(String languageCode);

    List<LocalizationSupportedLang> findAllByOrderByLanguageCodeAsc();
}
