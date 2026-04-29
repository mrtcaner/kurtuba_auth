package com.kurtuba.adm.controller;

import com.kurtuba.adm.data.dto.AdmUserDto;
import com.kurtuba.adm.data.dto.UserAdminSearchCriteria;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.UserRoleService;
import com.kurtuba.auth.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/auth/adm/pages/users")
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class UserAdminPageController {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final List<Integer> PAGE_SIZE_OPTIONS = List.of(25, 50, 100);

    private final UserService userService;
    private final UserRoleService userRoleService;

    public UserAdminPageController(UserService userService, UserRoleService userRoleService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
    }

    @GetMapping
    public String userPage(@RequestParam(name = "id", required = false, defaultValue = "") String id,
                           @RequestParam(name = "username", required = false, defaultValue = "") String username,
                           @RequestParam(name = "email", required = false, defaultValue = "") String email,
                           @RequestParam(name = "mobile", required = false, defaultValue = "") String mobile,
                           @RequestParam(name = "name", required = false, defaultValue = "") String name,
                           @RequestParam(name = "surname", required = false, defaultValue = "") String surname,
                           @RequestParam(name = "authProvider", required = false, defaultValue = "") String authProvider,
                           @RequestParam(name = "locale", required = false, defaultValue = "") String locale,
                           @RequestParam(name = "role", required = false, defaultValue = "") String role,
                           @RequestParam(name = "activated", required = false, defaultValue = "all") String activated,
                           @RequestParam(name = "locked", required = false, defaultValue = "all") String locked,
                           @RequestParam(name = "blocked", required = false, defaultValue = "all") String blocked,
                           @RequestParam(name = "showCaptcha", required = false, defaultValue = "all") String showCaptcha,
                           @RequestParam(name = "emailVerified", required = false, defaultValue = "all") String emailVerified,
                           @RequestParam(name = "mobileVerified", required = false, defaultValue = "all") String mobileVerified,
                           @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                           @RequestParam(name = "size", required = false, defaultValue = "25") int size,
                           Model model) {
        int pageSize = PAGE_SIZE_OPTIONS.contains(size) ? size : DEFAULT_PAGE_SIZE;
        int pageNumber = Math.max(page, 0);

        model.addAttribute("id", id);
        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("mobile", mobile);
        model.addAttribute("name", name);
        model.addAttribute("surname", surname);
        model.addAttribute("authProvider", authProvider);
        model.addAttribute("locale", locale);
        model.addAttribute("role", role);
        model.addAttribute("activated", activated);
        model.addAttribute("locked", locked);
        model.addAttribute("blocked", blocked);
        model.addAttribute("showCaptcha", showCaptcha);
        model.addAttribute("emailVerified", emailVerified);
        model.addAttribute("mobileVerified", mobileVerified);
        model.addAttribute("authProviders", AuthProviderType.values());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageSizeOptions", PAGE_SIZE_OPTIONS);

        UserAdminSearchCriteria criteria = UserAdminSearchCriteria.builder()
                .id(id)
                .username(username)
                .email(email)
                .mobile(mobile)
                .name(name)
                .surname(surname)
                .authProvider(authProvider)
                .locale(locale)
                .role(role)
                .activated(activated)
                .locked(locked)
                .blocked(blocked)
                .showCaptcha(showCaptcha)
                .emailVerified(emailVerified)
                .mobileVerified(mobileVerified)
                .build();

        Page<AdmUserDto> userPage = userService.searchAdmUsers(criteria,
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdDate")));
        if (userPage.getTotalPages() > 0 && pageNumber >= userPage.getTotalPages()) {
            pageNumber = userPage.getTotalPages() - 1;
            userPage = userService.searchAdmUsers(criteria,
                    PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdDate")));
        }

        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("pageNumbers", buildPageNumbers(userPage));
        return "adm/users/index";
    }

    @GetMapping("/{id}")
    public String userDetailPage(@PathVariable String id, Model model) {
        User user = userService.getUserById(id).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));
        model.addAttribute("user", user);
        model.addAttribute("availableRoles", AuthoritiesType.values());
        return "adm/users/detail";
    }

    @PostMapping("/{id}/roles")
    public String addRole(@PathVariable String id,
                          @RequestParam("roleName") String roleName,
                          RedirectAttributes redirectAttributes) {
        userRoleService.addRoleToUser(id, roleName);
        redirectAttributes.addFlashAttribute("successMessage", "Role added.");
        return "redirect:/auth/adm/pages/users/" + id;
    }

    @PostMapping("/{id}/roles/remove")
    public String removeRole(@PathVariable String id,
                             @RequestParam("roleName") String roleName,
                             RedirectAttributes redirectAttributes) {
        userRoleService.removeRoleFromUser(id, roleName);
        redirectAttributes.addFlashAttribute("successMessage", "Role removed.");
        return "redirect:/auth/adm/pages/users/" + id;
    }

    @PostMapping("/usernames/generate")
    public String generateMissingUsernames(RedirectAttributes redirectAttributes) {
        int generatedCount = userService.generateMissingUsernames();
        redirectAttributes.addFlashAttribute("successMessage",
                "Generated usernames for " + generatedCount + " users.");
        return "redirect:/auth/adm/pages/users";
    }

    @PostMapping("/{id}/security")
    public String updateSecurityAndActivity(@PathVariable String id,
                                            @RequestParam("activated") boolean activated,
                                            @RequestParam("locked") boolean locked,
                                            @RequestParam("blocked") boolean blocked,
                                            @RequestParam("showCaptcha") boolean showCaptcha,
                                            @RequestParam("failedLoginCount") int failedLoginCount,
                                            RedirectAttributes redirectAttributes) {
        userService.updateAdminSecurityAndActivity(id, activated, locked, blocked, showCaptcha, failedLoginCount);
        redirectAttributes.addFlashAttribute("successMessage", "Security and activity updated.");
        return "redirect:/auth/adm/pages/users/" + id;
    }

    private List<Integer> buildPageNumbers(Page<?> page) {
        if (page.getTotalPages() == 0) {
            return List.of();
        }
        int start = Math.max(0, page.getNumber() - 2);
        int end = Math.min(page.getTotalPages() - 1, page.getNumber() + 2);
        return IntStream.rangeClosed(start, end).boxed().toList();
    }
}
