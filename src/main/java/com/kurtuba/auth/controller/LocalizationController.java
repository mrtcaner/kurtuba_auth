package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.LocalizationMessageDto;
import com.kurtuba.auth.data.dto.LocalizationMessageResponseDto;
import com.kurtuba.auth.data.dto.LocalizationMessageUpdateDto;
import com.kurtuba.auth.data.model.LocalizationMessage;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.LocalizationService;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * todo this controller must be in adm service
 */
@RestController
@RequestMapping("auth")
public class LocalizationController {

    final
    LocalizationService localizationService;

    public LocalizationController(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @GetMapping("/localization")
    public ResponseEntity<List<LocalizationMessageResponseDto>> getLocalizations(@RequestParam(name = "lang", required = false) String lang,
                                                                                 @RequestParam(name = "key", required = false) String key){
        if(!StringUtils.hasLength(lang) && !StringUtils.hasLength(key)){
            return ResponseEntity.ok().body(localizationService.findAll().stream()
                    .map(localization -> LocalizationMessageResponseDto.fromLocalization(localization)).toList());
        }

        if (StringUtils.hasLength(lang) && StringUtils.hasLength(lang)){
            LocalizationMessage localizationMessage = localizationService.findByLanguageCodeAndKey(lang, key).orElse(null);
            if(localizationMessage == null){
                return ResponseEntity.ok().body(List.of());
            }

            return ResponseEntity.ok().body(List.of(LocalizationMessageResponseDto.fromLocalization(localizationMessage)));
        }

        if (StringUtils.hasLength(lang)){
            return ResponseEntity.ok().body(localizationService.findByLanguageCode(lang).stream()
                    .map(localization -> LocalizationMessageResponseDto.fromLocalization(localization)).toList());
        }

        return ResponseEntity.ok().body(localizationService.findByKey(key).stream()
                .map(localization -> LocalizationMessageResponseDto.fromLocalization(localization)).toList());

    }

    @PostMapping("/localization")
    public ResponseEntity createLocalization(@Valid @RequestBody LocalizationMessageDto localizationMessageDto){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LocalizationMessageResponseDto.fromLocalization(localizationService.create(localizationMessageDto)));
    }

    @PutMapping("/localization")
    public ResponseEntity updateLocalization(@Valid @RequestBody LocalizationMessageUpdateDto localizationMessageUpdateDto){
        // for the sake of proper cache management, changing lang and key is not allowed. So this swap needs to
        //take place
        LocalizationMessage localizationMessage = localizationService.finById(localizationMessageUpdateDto.getId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_ID));
        localizationMessage.setMessage(localizationMessageUpdateDto.getMessage());
        return ResponseEntity.ok()
                .body(LocalizationMessageResponseDto.fromLocalization(localizationService.update(LocalizationMessageDto
                        .fromLocalization(localizationMessage))));
    }

    @CacheEvict(value = "localization", allEntries = true)
    @DeleteMapping("/localization/cache")
    public ResponseEntity deleteLocalizationCache(){
        return ResponseEntity.ok().build();
    }

}
