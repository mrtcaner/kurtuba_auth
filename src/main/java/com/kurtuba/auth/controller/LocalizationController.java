package com.kurtuba.auth.controller;

import com.kurtuba.auth.data.dto.LocalizationDto;
import com.kurtuba.auth.data.dto.LocalizationResponseDto;
import com.kurtuba.auth.data.model.Localization;
import com.kurtuba.auth.service.LocalizationService;
import jakarta.validation.Valid;
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
    public ResponseEntity<List<LocalizationResponseDto>> getLocalizations(@RequestParam(name = "lang", required = false) String lang,
                                                                             @RequestParam(name = "key", required = false) String key){
        if(!StringUtils.hasLength(lang) && !StringUtils.hasLength(key)){
            return ResponseEntity.ok().body(localizationService.findAll().stream()
                    .map(localization -> LocalizationResponseDto.fromLocalization(localization)).toList());
        }

        if (StringUtils.hasLength(lang) && StringUtils.hasLength(lang)){
            Localization localization = localizationService.findByLanguageCodeAndKey(lang, key).orElse(null);
            if(localization == null){
                return ResponseEntity.ok().body(List.of());
            }

            return ResponseEntity.ok().body(List.of(LocalizationResponseDto.fromLocalization(localization)));
        }

        if (StringUtils.hasLength(lang)){
            return ResponseEntity.ok().body(localizationService.findByLanguageCode(lang).stream()
                    .map(localization -> LocalizationResponseDto.fromLocalization(localization)).toList());
        }

        return ResponseEntity.ok().body(localizationService.findByKey(key).stream()
                .map(localization -> LocalizationResponseDto.fromLocalization(localization)).toList());

    }

    @PostMapping("/localization")
    public ResponseEntity createLocalization(@Valid @RequestBody LocalizationDto localizationDto){
        return ResponseEntity.ok().body(localizationService.save(localizationDto));
    }

    @PutMapping("/localization")
    public ResponseEntity updateLocalization(@Valid @RequestBody LocalizationDto localizationDto){
        return ResponseEntity.ok().body(localizationService.update(localizationDto));
    }

}
