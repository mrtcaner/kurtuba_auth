package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.LocalizationMessageDto;
import com.kurtuba.auth.data.model.LocalizationMessage;
import com.kurtuba.auth.data.repository.LocalizationMessageRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
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
        return (List<LocalizationMessage>) localizationMessageRepository.findByLanguageCode(languageCode);
    }

    public List<LocalizationMessage> findByKey(String key) {
        return (List<LocalizationMessage>) localizationMessageRepository.findByKey(key);
    }

    @Cacheable(value = "localization", key = "#languageCode + '_' + #key")
    public Optional<LocalizationMessage> findByLanguageCodeAndKeyAndReturnOptional(String languageCode, String key) {
        return localizationMessageRepository.findByLanguageCodeAndKey(languageCode, key);
    }

    @Cacheable(value = "localization", key = "#languageCode + '_' + #key")
    public LocalizationMessage findByLanguageCodeAndKey(String languageCode, String key) {
        return localizationMessageRepository.findByLanguageCodeAndKey(languageCode, key).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER));
    }

    @CachePut(value = "localization", key = "#localizationMessageDto.languageCode + '_' + #localizationMessageDto.key")
    @Transactional
    public LocalizationMessage create(@Valid LocalizationMessageDto localizationMessageDto) {
        findByLanguageCodeAndKeyAndReturnOptional(localizationMessageDto.getLanguageCode(), localizationMessageDto.getKey())
                .ifPresent(localization -> {
            throw new BusinessLogicException(ErrorEnum.LOCALIZATION_ALREADY_EXISTS);
        });
        return localizationMessageRepository.save(LocalizationMessage.builder()

                .languageCode(localizationMessageDto.getLanguageCode())
                .key(localizationMessageDto.getKey())
                .message(localizationMessageDto.getMessage())
                .createdDate(LocalDateTime.now()).build());
    }

    @CacheEvict(value = "localization", key = "#localizationMessageDto.languageCode + '_' + #localizationMessageDto.key")
    @Transactional
    public LocalizationMessage update(@Valid LocalizationMessageDto localizationMessageDto) {
        LocalizationMessage localizationMessage = localizationMessageRepository.findById(localizationMessageDto.getId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_ID));
        return localizationMessageRepository.save(
                LocalizationMessage.builder()
                        .id(localizationMessage.getId())
                        .languageCode(localizationMessageDto.getLanguageCode())
                        .key(localizationMessageDto.getKey())
                        .message(localizationMessageDto.getMessage())
                        .createdDate(localizationMessage.getCreatedDate())
                        .updatedDate(LocalDateTime.now())
                        .build());
    }

}
