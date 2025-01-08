package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.LocalizationDto;
import com.kurtuba.auth.data.dto.LocalizationResponseDto;
import com.kurtuba.auth.data.model.Localization;
import com.kurtuba.auth.data.repository.LocalizationRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LocalizationService {

    final
    LocalizationRepository localizationRepository;


    public LocalizationService(LocalizationRepository localizationRepository) {
        this.localizationRepository = localizationRepository;
    }

    public List<Localization> findAll() {
        return (List<Localization>) localizationRepository.findAll();
    }

    public List<Localization> findByLanguageCode(String languageCode) {
        return (List<Localization>) localizationRepository.findByLanguageCode(languageCode);
    }

    public List<Localization> findByKey(String key) {
        return (List<Localization>) localizationRepository.findByKey(key);
    }

    @Cacheable("languageCode, key")
    public Optional<Localization> findByLanguageCodeAndKey(String languageCode, String key) {
        return localizationRepository.findByLanguageCodeAndKey(languageCode, key);
    }


    @Transactional
    public LocalizationResponseDto save(@Valid LocalizationDto localizationDto) {
        return LocalizationResponseDto.fromLocalization(localizationRepository.save(Localization.builder()
                .languageCode(localizationDto.getLanguageCode())
                .key(localizationDto.getKey())
                .message(localizationDto.getMessage())
                .createdDate(LocalDateTime.now()).build()));
    }

    @Transactional
    public LocalizationResponseDto update(@Valid LocalizationDto localizationDto) {
        Localization localization = localizationRepository.findById(localizationDto.getId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_ID));
        return LocalizationResponseDto.fromLocalization(localizationRepository.save(
                Localization.builder()
                        .id(localization.getId())
                        .languageCode(localizationDto.getLanguageCode())
                        .key(localizationDto.getKey())
                        .message(localizationDto.getMessage())
                        .updatedDate(LocalDateTime.now())
                        .build()));
    }

}
