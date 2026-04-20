package com.kurtuba.adm.controller;

import com.kurtuba.auth.data.model.UserToken;
import com.kurtuba.auth.service.UserTokenService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Controller
@RequestMapping("/auth/adm/pages/tokens")
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class TokenAdminPageController {

    private final UserTokenService userTokenService;

    public TokenAdminPageController(UserTokenService userTokenService) {
        this.userTokenService = userTokenService;
    }

    @GetMapping
    public String tokenPage(@RequestParam(name = "userId", required = false, defaultValue = "") String userId,
                            @RequestParam(name = "clientId", required = false, defaultValue = "") String clientId,
                            @RequestParam(name = "jti", required = false, defaultValue = "") String jti,
                            @RequestParam(name = "blocked", required = false, defaultValue = "all") String blocked,
                            @RequestParam(name = "refreshTokenUsed", required = false, defaultValue = "all") String refreshTokenUsed,
                            @RequestParam(name = "sortBy", required = false, defaultValue = "createdDate") String sortBy,
                            @RequestParam(name = "sortDir", required = false, defaultValue = "desc") String sortDir,
                            Model model) {
        model.addAttribute("userId", userId);
        model.addAttribute("clientId", clientId);
        model.addAttribute("jti", jti);
        model.addAttribute("blocked", blocked);
        model.addAttribute("refreshTokenUsed", refreshTokenUsed);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        List<UserToken> tokens = List.of();
        if (StringUtils.hasLength(userId)) {
            tokens = userTokenService.findAllByUserId(userId).stream()
                    .filter(token -> !StringUtils.hasLength(clientId) || clientId.equals(token.getClientId()))
                    .filter(token -> matchesBooleanFilter(blocked, token.isBlocked()))
                    .filter(token -> matchesBooleanFilter(refreshTokenUsed, token.isRefreshTokenUsed()))
                    .sorted(resolveComparator(sortBy, sortDir))
                    .toList();
        }

        Optional<UserToken> tokenByJti = Optional.empty();
        Boolean jtiBlocked = null;
        if (StringUtils.hasLength(jti)) {
            tokenByJti = userTokenService.findByJTI(jti);
            jtiBlocked = userTokenService.checkDBIfTokenIsBlockedByJTI(jti);
        }

        model.addAttribute("tokens", tokens);
        model.addAttribute("tokenByJti", tokenByJti.orElse(null));
        model.addAttribute("jtiBlocked", jtiBlocked);
        return "adm/tokens/index";
    }

    @PostMapping("/block-user")
    public String blockUsersTokens(@RequestParam("userId") String userId,
                                   @RequestParam(name = "clientId", required = false, defaultValue = "") String clientId,
                                   @RequestParam(name = "blocked", required = false, defaultValue = "all") String blocked,
                                   @RequestParam(name = "refreshTokenUsed", required = false, defaultValue = "all") String refreshTokenUsed,
                                   @RequestParam(name = "sortBy", required = false, defaultValue = "createdDate") String sortBy,
                                   @RequestParam(name = "sortDir", required = false, defaultValue = "desc") String sortDir,
                                   RedirectAttributes redirectAttributes) {
        userTokenService.blockUsersTokens(userId);
        redirectAttributes.addFlashAttribute("successMessage", "All tokens for the user have been blocked.");
        redirectAttributes.addAttribute("userId", userId);
        if (StringUtils.hasLength(clientId)) {
            redirectAttributes.addAttribute("clientId", clientId);
        }
        if (!"all".equalsIgnoreCase(blocked)) {
            redirectAttributes.addAttribute("blocked", blocked);
        }
        if (!"all".equalsIgnoreCase(refreshTokenUsed)) {
            redirectAttributes.addAttribute("refreshTokenUsed", refreshTokenUsed);
        }
        redirectAttributes.addAttribute("sortBy", sortBy);
        redirectAttributes.addAttribute("sortDir", sortDir);
        return "redirect:/auth/adm/pages/tokens";
    }

    @PostMapping("/block-jti")
    public String blockTokenByJti(@RequestParam("jti") String jti,
                                  @RequestParam(name = "userId", required = false, defaultValue = "") String userId,
                                  @RequestParam(name = "clientId", required = false, defaultValue = "") String clientId,
                                  @RequestParam(name = "blocked", required = false, defaultValue = "all") String blocked,
                                  @RequestParam(name = "refreshTokenUsed", required = false, defaultValue = "all") String refreshTokenUsed,
                                  @RequestParam(name = "sortBy", required = false, defaultValue = "createdDate") String sortBy,
                                  @RequestParam(name = "sortDir", required = false, defaultValue = "desc") String sortDir,
                                  RedirectAttributes redirectAttributes) {
        userTokenService.changeTokenBlockByJTI(List.of(jti), true);
        redirectAttributes.addFlashAttribute("successMessage", "Token has been blocked.");
        if (StringUtils.hasLength(userId)) {
            redirectAttributes.addAttribute("userId", userId);
        }
        if (StringUtils.hasLength(clientId)) {
            redirectAttributes.addAttribute("clientId", clientId);
        }
        if (!"all".equalsIgnoreCase(blocked)) {
            redirectAttributes.addAttribute("blocked", blocked);
        }
        if (!"all".equalsIgnoreCase(refreshTokenUsed)) {
            redirectAttributes.addAttribute("refreshTokenUsed", refreshTokenUsed);
        }
        redirectAttributes.addAttribute("sortBy", sortBy);
        redirectAttributes.addAttribute("sortDir", sortDir);
        redirectAttributes.addAttribute("jti", jti);
        return "redirect:/auth/adm/pages/tokens";
    }

    private boolean matchesBooleanFilter(String filterValue, boolean actualValue) {
        return switch (filterValue == null ? "all" : filterValue.toLowerCase()) {
            case "yes" -> actualValue;
            case "no" -> !actualValue;
            default -> true;
        };
    }

    private Comparator<UserToken> resolveComparator(String sortBy, String sortDir) {
        Comparator<UserToken> comparator = switch (sortBy) {
            case "blocked" -> Comparator.comparing(UserToken::isBlocked);
            case "refreshTokenUsed" -> Comparator.comparing(UserToken::isRefreshTokenUsed);
            case "accessTokenExp" -> Comparator.comparing(UserToken::getExpirationDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "refreshTokenExp" -> Comparator.comparing(UserToken::getRefreshTokenExp,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "createdDate" -> Comparator.comparing(UserToken::getCreatedDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(UserToken::getCreatedDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };

        if (!"asc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return comparator.thenComparing(UserToken::getClientId, Comparator.nullsLast(String::compareToIgnoreCase));
    }
}
