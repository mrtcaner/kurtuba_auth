package com.kurtuba.adm.controller;

import com.kurtuba.auth.data.dto.LocalizationMessageDto;
import com.kurtuba.auth.data.dto.LocalizationMessageResponseDto;
import com.kurtuba.auth.data.dto.LocalizationMessageUpdateDto;
import com.kurtuba.auth.data.model.LocalizationMessage;
import com.kurtuba.auth.data.model.LocalizationSupportedCountry;
import com.kurtuba.auth.data.model.LocalizationSupportedLang;
import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.LocalizationMessageService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/auth/adm/pages/localization")
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class LocalizationAdminPageController {

    private final LocalizationMessageService localizationMessageService;
    private final LocalizationSupportedLangRepository localizationSupportedLangRepository;
    private final LocalizationSupportedCountryRepository localizationSupportedCountryRepository;

    public LocalizationAdminPageController(LocalizationMessageService localizationMessageService,
                                           LocalizationSupportedLangRepository localizationSupportedLangRepository,
                                           LocalizationSupportedCountryRepository localizationSupportedCountryRepository) {
        this.localizationMessageService = localizationMessageService;
        this.localizationSupportedLangRepository = localizationSupportedLangRepository;
        this.localizationSupportedCountryRepository = localizationSupportedCountryRepository;
    }

    @GetMapping
    public String localizationRoot() {
        return "redirect:/auth/adm/pages/localization/messages";
    }

    @GetMapping("/messages")
    public String localizationPage(@RequestParam(name = "filterLang", required = false, defaultValue = "") String filterLang,
                                   @RequestParam(name = "filterKey", required = false, defaultValue = "") String filterKey,
                                   @RequestParam(name = "filterMessage", required = false, defaultValue = "") String filterMessage,
                                   Model model) {
        model.addAttribute("filterLang", filterLang);
        model.addAttribute("filterKey", filterKey);
        model.addAttribute("filterMessage", filterMessage);
        addMessagesModel(model, resolveMessages(filterLang, filterKey, filterMessage));
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", LocalizationMessageDto.builder().build());
        }
        return "adm/localization/messages";
    }

    @PostMapping("/messages")
    public String createLocalization(@Valid @ModelAttribute("createForm") LocalizationMessageDto createForm,
                                     BindingResult bindingResult,
                                     @ModelAttribute("filterLang") String filterLang,
                                     @ModelAttribute("filterKey") String filterKey,
                                     @ModelAttribute("filterMessage") String filterMessage,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addMessagesModel(model, resolveMessages(filterLang, filterKey, filterMessage));
            return "adm/localization/messages";
        }

        localizationMessageService.create(createForm);
        redirectAttributes.addFlashAttribute("successMessage", "Localization created.");
        redirectAttributes.addFlashAttribute("filterLang", createForm.getLanguageCode());
        redirectAttributes.addFlashAttribute("filterKey", createForm.getKey());
        redirectAttributes.addFlashAttribute("filterMessage", "");
        return "redirect:/auth/adm/pages/localization/messages";
    }

    @PostMapping("/messages/{id}")
    public String updateLocalization(@PathVariable String id,
                                     @Valid @ModelAttribute("updateForm") LocalizationMessageUpdateDto updateForm,
                                     BindingResult bindingResult,
                                     @ModelAttribute("filterLang") String filterLang,
                                     @ModelAttribute("filterKey") String filterKey,
                                     @ModelAttribute("filterMessage") String filterMessage,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Message update failed.");
            redirectAttributes.addFlashAttribute("filterLang", filterLang);
            redirectAttributes.addFlashAttribute("filterKey", filterKey);
            redirectAttributes.addFlashAttribute("filterMessage", filterMessage);
            return "redirect:/auth/adm/pages/localization/messages";
        }

        LocalizationMessage localizationMessage = localizationMessageService.finById(id).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_ID));
        localizationMessage.setMessage(updateForm.getMessage());
        localizationMessageService.update(LocalizationMessageDto.fromLocalization(localizationMessage));

        redirectAttributes.addFlashAttribute("successMessage", "Localization updated.");
        redirectAttributes.addFlashAttribute("filterLang",
                StringUtils.hasLength(filterLang) ? filterLang : localizationMessage.getLanguageCode());
        redirectAttributes.addFlashAttribute("filterKey",
                StringUtils.hasLength(filterKey) ? filterKey : localizationMessage.getMessageKey());
        redirectAttributes.addFlashAttribute("filterMessage", filterMessage);
        return "redirect:/auth/adm/pages/localization/messages";
    }

    @PostMapping("/messages/{id}/delete")
    public String deleteLocalization(@PathVariable String id,
                                     @ModelAttribute("filterLang") String filterLang,
                                     @ModelAttribute("filterKey") String filterKey,
                                     @ModelAttribute("filterMessage") String filterMessage,
                                     RedirectAttributes redirectAttributes) {
        localizationMessageService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Localization deleted.");
        redirectAttributes.addFlashAttribute("filterLang", filterLang);
        redirectAttributes.addFlashAttribute("filterKey", filterKey);
        redirectAttributes.addFlashAttribute("filterMessage", filterMessage);
        return "redirect:/auth/adm/pages/localization/messages";
    }

    @GetMapping("/languages")
    public String languagesPage(Model model) {
        List<LocalizationSupportedLang> supportedLanguages = localizationSupportedLangRepository.findAllByOrderByLanguageCodeAsc();
        model.addAttribute("supportedLanguages", supportedLanguages);
        model.addAttribute("hasSupportedLanguages", !supportedLanguages.isEmpty());
        return "adm/localization/languages";
    }

    @PostMapping("/languages")
    public String createSupportedLanguage(@RequestParam("languageCode") String languageCode,
                                          RedirectAttributes redirectAttributes) {
        String normalizedLanguageCode = normalizeCode(languageCode);
        if (!StringUtils.hasLength(normalizedLanguageCode)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Language code is required.");
            return "redirect:/auth/adm/pages/localization/languages";
        }
        if (localizationSupportedLangRepository.findByLanguageCode(normalizedLanguageCode).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Language already exists.");
            return "redirect:/auth/adm/pages/localization/languages";
        }

        localizationSupportedLangRepository.save(LocalizationSupportedLang.builder()
                .languageCode(normalizedLanguageCode)
                .createdDate(Instant.now())
                .build());
        redirectAttributes.addFlashAttribute("successMessage", "Supported language added.");
        return "redirect:/auth/adm/pages/localization/languages";
    }

    @PostMapping("/languages/{id}/delete")
    public String deleteSupportedLanguage(@PathVariable String id, RedirectAttributes redirectAttributes) {
        localizationSupportedLangRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supported language deleted.");
        return "redirect:/auth/adm/pages/localization/languages";
    }

    @GetMapping("/countries")
    public String countriesPage(Model model) {
        List<LocalizationSupportedCountry> supportedCountries = localizationSupportedCountryRepository.findAllByOrderByCountryCodeAsc();
        model.addAttribute("supportedCountries", supportedCountries);
        model.addAttribute("hasSupportedCountries", !supportedCountries.isEmpty());
        return "adm/localization/countries";
    }

    @PostMapping("/countries")
    public String createSupportedCountry(@RequestParam("countryCode") String countryCode,
                                         RedirectAttributes redirectAttributes) {
        String normalizedCountryCode = normalizeCode(countryCode);
        if (!StringUtils.hasLength(normalizedCountryCode)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Country code is required.");
            return "redirect:/auth/adm/pages/localization/countries";
        }
        if (localizationSupportedCountryRepository.findByCountryCode(normalizedCountryCode).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Country already exists.");
            return "redirect:/auth/adm/pages/localization/countries";
        }

        localizationSupportedCountryRepository.save(LocalizationSupportedCountry.builder()
                .countryCode(normalizedCountryCode)
                .createdDate(Instant.now())
                .build());
        redirectAttributes.addFlashAttribute("successMessage", "Supported country added.");
        return "redirect:/auth/adm/pages/localization/countries";
    }

    @PostMapping("/countries/{id}/delete")
    public String deleteSupportedCountry(@PathVariable String id, RedirectAttributes redirectAttributes) {
        localizationSupportedCountryRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supported country deleted.");
        return "redirect:/auth/adm/pages/localization/countries";
    }

    private List<LocalizationMessageResponseDto> resolveMessages(String filterLang, String filterKey, String filterMessage) {
        return localizationMessageService.search(filterLang, filterKey, filterMessage).stream()
                .map(LocalizationMessageResponseDto::fromLocalization)
                .toList();
    }

    private void addMessagesModel(Model model, List<LocalizationMessageResponseDto> messages) {
        model.addAttribute("messages", messages);
        model.addAttribute("hasMessages", !messages.isEmpty());
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }
}
