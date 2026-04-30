package com.kurtuba.adm.controller;

import com.kurtuba.adm.data.dto.AdmUserFcmTokenSearchCriteria;
import com.kurtuba.auth.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/auth/adm/pages/push-notifications")
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class PushNotificationAdminPageController {

    private final UserService userService;

    public PushNotificationAdminPageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String pushNotificationPage(@RequestParam(name = "userId", required = false, defaultValue = "") String userId,
                                       @RequestParam(name = "userEmail", required = false, defaultValue = "") String userEmail,
                                       @RequestParam(name = "userMobile", required = false, defaultValue = "") String userMobile,
                                       @RequestParam(name = "userRole", required = false, defaultValue = "") String userRole,
                                       @RequestParam(name = "firebaseInstallationId", required = false, defaultValue = "") String firebaseInstallationId,
                                       @RequestParam(name = "fcmToken", required = false, defaultValue = "") String fcmToken,
                                       Model model) {
        model.addAttribute("userId", userId);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("userMobile", userMobile);
        model.addAttribute("userRole", userRole);
        model.addAttribute("firebaseInstallationId", firebaseInstallationId);
        model.addAttribute("fcmToken", fcmToken);

        List<?> fcmTokens = userService.searchAdmUserFcmTokens(AdmUserFcmTokenSearchCriteria.builder()
                .userId(userId)
                .userEmail(userEmail)
                .userMobile(userMobile)
                .userRole(userRole)
                .firebaseInstallationId(firebaseInstallationId)
                .fcmToken(fcmToken)
                .build());
        model.addAttribute("fcmTokens", fcmTokens);
        model.addAttribute("fcmTokenCount", fcmTokens.size());
        model.addAttribute("hasFcmTokens", !fcmTokens.isEmpty());
        return "adm/push-notifications/index";
    }
}
