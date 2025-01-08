package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.LocalizationMessage;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface LocalizationMessageRepository extends CrudRepository<LocalizationMessage, String> {

    Optional<LocalizationMessage> findByLanguageCodeAndKey(String language, String key);

    List<LocalizationMessage> findByLanguageCode(String languageCode);

    List<LocalizationMessage> findByKey(String languageCode);

}
