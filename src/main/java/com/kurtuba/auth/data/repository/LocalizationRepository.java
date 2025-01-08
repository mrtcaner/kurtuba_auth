package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.Localization;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface LocalizationRepository extends CrudRepository<Localization, String> {

    Optional<Localization> findByLanguageCodeAndKey(String language, String key);

    List<Localization> findByLanguageCode(String languageCode);

    List<Localization> findByKey(String languageCode);

}
