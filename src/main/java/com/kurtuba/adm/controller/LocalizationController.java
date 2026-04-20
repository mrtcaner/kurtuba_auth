package com.kurtuba.adm.controller;

import com.kurtuba.auth.data.dto.LocalizationMessageDto;
import com.kurtuba.auth.data.dto.LocalizationMessageResponseDto;
import com.kurtuba.auth.data.dto.LocalizationMessageUpdateDto;
import com.kurtuba.auth.data.model.LocalizationMessage;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.LocalizationMessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/auth/adm")
public class LocalizationController {

    final
    LocalizationMessageService localizationMessageService;

    public LocalizationController(LocalizationMessageService localizationMessageService) {
        this.localizationMessageService = localizationMessageService;
    }

    @GetMapping("/localization")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<List<LocalizationMessageResponseDto>> getLocalizations(@RequestParam(name = "lang", required = false) String lang,
                                                                                 @RequestParam(name = "key", required = false) String key,
                                                                                 @RequestParam(name = "message", required = false) String message){
        return ResponseEntity.ok().body(localizationMessageService.search(lang, key, message).stream()
                .map(localization -> LocalizationMessageResponseDto.fromLocalization(localization)).toList());
    }

    @PostMapping("/localization")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity createLocalization(@Valid @RequestBody LocalizationMessageDto localizationMessageDto){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LocalizationMessageResponseDto.fromLocalization(localizationMessageService.create(localizationMessageDto)));
    }

    @PutMapping("/localization")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity updateLocalization(@Valid @RequestBody LocalizationMessageUpdateDto localizationMessageUpdateDto){
        LocalizationMessage localizationMessage = localizationMessageService.finById(localizationMessageUpdateDto.getId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_ID));
        localizationMessage.setMessage(localizationMessageUpdateDto.getMessage());
        return ResponseEntity.ok()
                .body(LocalizationMessageResponseDto.fromLocalization(localizationMessageService.update(LocalizationMessageDto
                        .fromLocalization(localizationMessage))));
    }

    @DeleteMapping("/localization/{id}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Void> deleteLocalization(@PathVariable String id){
        localizationMessageService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
