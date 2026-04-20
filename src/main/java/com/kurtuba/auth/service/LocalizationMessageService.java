package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.LocalizationMessageDto;
import com.kurtuba.auth.data.model.LocalizationMessage;
import com.kurtuba.auth.data.repository.LocalizationMessageRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Validated
public class LocalizationMessageService {

    final
    LocalizationMessageRepository localizationMessageRepository;

    public LocalizationMessageService(LocalizationMessageRepository localizationMessageRepository) {
        this.localizationMessageRepository = localizationMessageRepository;
    }

    public Optional<LocalizationMessage> finById(String id) {
        return localizationMessageRepository.findById(id);
    }

    public List<LocalizationMessage> findAll() {
        return (List<LocalizationMessage>) localizationMessageRepository.findAll();
    }

    public List<LocalizationMessage> findByLanguageCode(String languageCode) {
        return localizationMessageRepository.findByLanguageCode(languageCode);
    }

    public List<LocalizationMessage> findByKey(String key) {
        return localizationMessageRepository.findByMessageKeyContainingIgnoreCase(key);
    }

    public Optional<LocalizationMessage> findByLanguageCodeAndKeyAndReturnOptional(String languageCode, String key) {
        return localizationMessageRepository.findByLanguageCodeAndMessageKey(languageCode, key);
    }

    public LocalizationMessage findByLanguageCodeAndMessageKey(String languageCode, String messageKey) {
        return localizationMessageRepository.findByLanguageCodeAndMessageKey(languageCode, messageKey).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER));
    }

    @Transactional
    public LocalizationMessage create(@Valid LocalizationMessageDto localizationMessageDto) {
        findByLanguageCodeAndKeyAndReturnOptional(localizationMessageDto.getLanguageCode(), localizationMessageDto.getKey())
                .ifPresent(localization -> {
            throw new BusinessLogicException(ErrorEnum.LOCALIZATION_ALREADY_EXISTS);
        });
        return localizationMessageRepository.save(LocalizationMessage.builder()

                .languageCode(localizationMessageDto.getLanguageCode())
                .messageKey(localizationMessageDto.getKey())
                .message(localizationMessageDto.getMessage())
                .createdDate(Instant.now()).build());
    }

    @Transactional
    public LocalizationMessage update(@Valid LocalizationMessageDto localizationMessageDto) {
        LocalizationMessage localizationMessage = localizationMessageRepository.findById(localizationMessageDto.getId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_ID));
        return localizationMessageRepository.save(
                LocalizationMessage.builder()
                        .id(localizationMessage.getId())
                        .languageCode(localizationMessageDto.getLanguageCode())
                        .messageKey(localizationMessageDto.getKey())
                        .message(localizationMessageDto.getMessage())
                        .createdDate(localizationMessage.getCreatedDate())
                        .updatedDate(Instant.now())
                        .build());
    }

    @Transactional
    public void deleteById(String id) {
        LocalizationMessage localizationMessage = localizationMessageRepository.findById(id).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_ID));
        localizationMessageRepository.delete(localizationMessage);
    }

    public List<LocalizationMessage> search(String languageCode, String key, String message) {
        boolean hasLanguageCode = StringUtils.hasLength(languageCode);
        boolean hasKey = StringUtils.hasLength(key);
        boolean hasMessage = StringUtils.hasLength(message);

        if (!hasLanguageCode && !hasKey && !hasMessage) {
            return findAll();
        }
        if (hasLanguageCode && hasKey && hasMessage) {
            return localizationMessageRepository.findByLanguageCodeAndMessageKeyContainingIgnoreCaseAndMessageContainingIgnoreCase(
                    languageCode, key, message);
        }
        if (hasLanguageCode && hasKey) {
            return localizationMessageRepository.findByLanguageCodeAndMessageKeyContainingIgnoreCase(languageCode, key);
        }
        if (hasLanguageCode && hasMessage) {
            return localizationMessageRepository.findByLanguageCodeAndMessageContainingIgnoreCase(languageCode, message);
        }
        if (hasKey && hasMessage) {
            return localizationMessageRepository.findByMessageKeyContainingIgnoreCaseAndMessageContainingIgnoreCase(key, message);
        }
        if (hasLanguageCode) {
            return localizationMessageRepository.findByLanguageCode(languageCode);
        }
        if (hasKey) {
            return localizationMessageRepository.findByMessageKeyContainingIgnoreCase(key);
        }
        return localizationMessageRepository.findByMessageContainingIgnoreCase(message);
    }

}
