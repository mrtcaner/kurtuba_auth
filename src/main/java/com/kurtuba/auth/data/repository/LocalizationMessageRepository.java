package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.LocalizationMessage;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface LocalizationMessageRepository extends CrudRepository<LocalizationMessage, String> {

    Optional<LocalizationMessage> findByLanguageCodeAndMessageKey(String language, String key);

    List<LocalizationMessage> findByLanguageCode(String languageCode);

    List<LocalizationMessage> findByMessageKeyContainingIgnoreCase(String key);

    List<LocalizationMessage> findByMessageContainingIgnoreCase(String message);

    List<LocalizationMessage> findByLanguageCodeAndMessageKeyContainingIgnoreCase(String languageCode, String key);

    List<LocalizationMessage> findByLanguageCodeAndMessageContainingIgnoreCase(String languageCode, String message);

    List<LocalizationMessage> findByMessageKeyContainingIgnoreCaseAndMessageContainingIgnoreCase(String key, String message);

    List<LocalizationMessage> findByLanguageCodeAndMessageKeyContainingIgnoreCaseAndMessageContainingIgnoreCase(String languageCode,
                                                                                                                  String key,
                                                                                                                  String message);

}
