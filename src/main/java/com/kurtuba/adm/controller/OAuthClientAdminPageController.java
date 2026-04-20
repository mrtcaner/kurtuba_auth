package com.kurtuba.adm.controller;

import com.kurtuba.adm.data.dto.RegisteredClientFormDto;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/auth/adm/oauth-clients")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SCOPE_ADMIN')")
public class OAuthClientAdminPageController {

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuthClientAdminPageController(RegisteredClientRepository registeredClientRepository,
                                          PasswordEncoder passwordEncoder) {
        this.registeredClientRepository = registeredClientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listClients(Model model) {
        model.addAttribute("clients", StreamSupport.stream(registeredClientRepository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(RegisteredClient::getClientName, String.CASE_INSENSITIVE_ORDER))
                .toList());
        return "adm/oauth-clients/list";
    }

    @GetMapping("/new")
    public String newClient(Model model) {
        model.addAttribute("form", RegisteredClientFormDto.builder()
                .accessTokenTtlMinutes(60)
                .refreshTokenEnabled(true)
                .refreshTokenTtlMinutes(10080)
                .cookieHttpOnly(true)
                .build());
        model.addAttribute("isNewClient", true);
        return "adm/oauth-clients/form";
    }

    @GetMapping("/{id}")
    public String editClient(@PathVariable String id, Model model) {
        RegisteredClient client = findClient(id);
        model.addAttribute("form", RegisteredClientFormDto.fromRegisteredClient(client));
        model.addAttribute("isNewClient", false);
        return "adm/oauth-clients/form";
    }

    @PostMapping
    public String createClient(@Valid @ModelAttribute("form") RegisteredClientFormDto form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        validateBusinessRules(form, bindingResult, Optional.empty());
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNewClient", true);
            return "adm/oauth-clients/form";
        }

        RegisteredClient savedClient = registeredClientRepository.save(form.toRegisteredClient(encodedSecret(form, null)));
        redirectAttributes.addFlashAttribute("successMessage",
                "OAuth client created. Store the raw client secret now if you entered one.");
        return "redirect:/auth/adm/oauth-clients/" + savedClient.getId();
    }

    @PostMapping("/{id}")
    public String updateClient(@PathVariable String id,
                               @Valid @ModelAttribute("form") RegisteredClientFormDto form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        RegisteredClient existingClient = findClient(id);
        form.setId(existingClient.getId());
        form.setCreatedDate(existingClient.getCreatedDate());

        validateBusinessRules(form, bindingResult, Optional.of(existingClient));
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNewClient", false);
            return "adm/oauth-clients/form";
        }

        registeredClientRepository.save(form.toRegisteredClient(encodedSecret(form, existingClient)));
        redirectAttributes.addFlashAttribute("successMessage", "OAuth client updated.");
        return "redirect:/auth/adm/oauth-clients/" + id;
    }

    @ModelAttribute("clientTypes")
    public List<RegisteredClientType> clientTypes() {
        return List.of(RegisteredClientType.values());
    }

    private RegisteredClient findClient(String id) {
        return registeredClientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    private String encodedSecret(RegisteredClientFormDto form, RegisteredClient existingClient) {
        if (form.getRawClientSecret() != null && !form.getRawClientSecret().isBlank()) {
            return passwordEncoder.encode(form.getRawClientSecret().trim());
        }

        return existingClient == null ? null : existingClient.getClientSecret();
    }

    private void validateBusinessRules(RegisteredClientFormDto form,
                                       BindingResult bindingResult,
                                       Optional<RegisteredClient> existingClient) {
        if (form.getCreatedDate() == null) {
            form.setCreatedDate(Instant.now());
        }

        if (form.getClientId() != null) {
            registeredClientRepository.findByClientId(form.getClientId().trim())
                    .filter(client -> existingClient.map(value -> !value.getId().equals(client.getId())).orElse(true))
                    .ifPresent(client -> bindingResult.rejectValue("clientId", "duplicate", "Client ID must be unique."));
        }

        if (form.getClientName() != null) {
            registeredClientRepository.findByClientName(form.getClientName().trim())
                    .filter(client -> existingClient.map(value -> !value.getId().equals(client.getId())).orElse(true))
                    .ifPresent(client -> bindingResult.rejectValue("clientName", "duplicate", "Client name must be unique."));
        }

        if (RegisteredClientType.SERVICE.equals(form.getClientType())
                && existingClient.isEmpty()
                && (form.getRawClientSecret() == null || form.getRawClientSecret().isBlank())) {
            bindingResult.rejectValue("rawClientSecret", "required", "Service clients need a client secret.");
        }

        if (!form.isRefreshTokenEnabled()) {
            form.setRefreshTokenTtlMinutes(0);
        }

        if (!form.isSendTokenInCookie()) {
            form.setCookieHttpOnly(false);
            form.setCookieSecure(false);
            form.setCookieMaxAgeSeconds(0);
        }
    }
}
