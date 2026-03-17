package com.kurtuba;

import com.kurtuba.auth.data.dto.LocalizationMessageDto;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.*;
import com.kurtuba.auth.data.repository.LocalizationAvailableLocaleRepository;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.data.repository.RoleRepository;
import com.kurtuba.auth.data.repository.UserSettingRepository;
import com.kurtuba.auth.service.LocalizationMessageService;
import com.kurtuba.auth.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class BootstrapCLR implements CommandLineRunner {

    private final UserService userService;

    private final RegisteredClientRepository registeredClientRepository;

    private final LocalizationMessageService localizationMessageService;

    private final LocalizationAvailableLocaleRepository localizationAvailableLocaleRepository;

    private final RoleRepository roleRepository;

    private final UserSettingRepository userSettingRepository;

    public BootstrapCLR(UserService userService, RegisteredClientRepository registeredClientRepository,
                        LocalizationMessageService localizationMessageService,
                        LocalizationAvailableLocaleRepository localizationAvailableLocaleRepository,
                        RoleRepository roleRepository, UserSettingRepository userSettingRepository) {
        this.userService = userService;
        this.registeredClientRepository = registeredClientRepository;
        this.localizationMessageService = localizationMessageService;
        this.localizationAvailableLocaleRepository = localizationAvailableLocaleRepository;
        this.roleRepository = roleRepository;
        this.userSettingRepository = userSettingRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("Running BootstrapCLR");
        seedLocales();
        seedLocalizationMessages();
        seedRoles();
        seedRegisteredClients();
        seedUsers();
    }

    private void seedLocales() {
        seedLocale("en", "tr");
        seedLocale("tr", "tr");
        seedLocale("en", "us");
    }

    private void seedLocale(String languageCode, String countryCode) {
        localizationAvailableLocaleRepository.findByLanguageCodeAndCountryCode(languageCode, countryCode)
                                             .orElseGet(() -> localizationAvailableLocaleRepository.save(
                                                     LocalizationAvailableLocale.builder()
                                                                                .languageCode(languageCode)
                                                                                .countryCode(countryCode)
                                                                                .createdDate(Instant.now())
                                                                                .build()));
    }

    private void seedLocalizationMessages() {
        Map<String, String> messages = Map.ofEntries(
                Map.entry("mail.account.activation.subject", "Kurtuba Account Activation"),
                Map.entry("mail.account.activation.content.title", "Thanks for signing up!"),
                Map.entry("mail.account.activation.content.greet", "Hi"),
                Map.entry("mail.account.activation.content.code.prologue", "Your activation code is below."),
                Map.entry("mail.account.activation.content.code.epilogue", "Use it to activate your account."),
                Map.entry("mail.account.activation.content.link.prologue", "Click below to activate your account."),
                Map.entry("mail.account.activation.content.link.button.label", "Activate Account"),
                Map.entry("mail.account.activation.content.closing", "Cheers"),
                Map.entry("mail.account.activation.content.closing.subject", "Kurtuba Team"),
                Map.entry("mail.account.activation.content.get-in-touch", "Get in touch"),
                Map.entry("mail.password.reset.subject", "Kurtuba Password Reset"),
                Map.entry("mail.password.reset.content.code.prologue", "Your password reset code is below."),
                Map.entry("mail.password.reset.content.link.prologue", "Click below to reset your password."),
                Map.entry("mail.password.reset.content.epilogue", "If you did not request this, ignore this message."),
                Map.entry("mail.password.reset.content.closing", "Cheers"),
                Map.entry("mail.password.reset.content.closing.subject", "Kurtuba Team"),
                Map.entry("mail.password.reset.content.get-in-touch", "Get in touch"),
                Map.entry("mail.email.verification.subject", "Verify Your Email"),
                Map.entry("mail.email.verification.content.title", "Verify your email"),
                Map.entry("mail.email.verification.content.greet", "Hi"),
                Map.entry("mail.email.verification.content.code.prologue", "Your verification code is below."),
                Map.entry("mail.email.verification.content.link.prologue", "Click below to verify your email."),
                Map.entry("mail.email.verification.content.link.button.label", "Verify Email"),
                Map.entry("mail.email.verification.content.closing", "Cheers"),
                Map.entry("mail.email.verification.content.closing.subject", "Kurtuba Team"),
                Map.entry("mail.email.verification.content.get-in-touch", "Get in touch"),
                Map.entry("mail.account.modification.subject", "Account Update"),
                Map.entry("mail.account.modification.content.greet", "Hi"),
                Map.entry("mail.account.modification.content.prologue", "Your account was updated."),
                Map.entry("mail.account.modification.content.context", "Updated field"),
                Map.entry("mail.account.modification.content.epilogue", "If this was not you, contact support."),
                Map.entry("mail.account.modification.content.closing", "Cheers"),
                Map.entry("mail.account.modification.content.closing.subject", "Kurtuba Team"),
                Map.entry("mail.account.modification.content.get-in-touch", "Get in touch"));

        messages.forEach((key, value) -> localizationMessageService.findByLanguageCodeAndKeyAndReturnOptional("en", key)
                                                                   .orElseGet(() -> localizationMessageService.create(
                                                                           LocalizationMessageDto.builder()
                                                                                                 .languageCode("en")
                                                                                                 .key(key)
                                                                                                 .message(value)
                                                                                                 .build())));
    }

    private void seedRoles() {
        seedRole(AuthoritiesType.USER.name());
        seedRole(AuthoritiesType.ADMIN.name());
        seedRole(AuthoritiesType.SERVICE.name());
    }

    private void seedRole(String roleName) {
        roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

    private void seedRegisteredClients() {
        seedClient("default-client", "96939331-32b9-4089-a121-934de609f5df", RegisteredClientType.DEFAULT, null,
                   Set.of("http://localhost:8080"), false, null, 60, true, 10080, false, 0);
        seedClient("demo-web-client", UUID.randomUUID().toString(), RegisteredClientType.WEB, null,
                   Set.of("http://localhost:8080"), true, Set.of(AuthoritiesType.USER.name()), 60, true, 10080, true,
                   86400);
        seedClient("demo-service-client", UUID.randomUUID().toString(), RegisteredClientType.SERVICE,
                   "demo-service-secret", Set.of("http://localhost:8080"), true, Set.of(AuthoritiesType.SERVICE.name()),
                   60, false, 0, false, 0);
    }

    private void seedClient(String clientName, String clientId, RegisteredClientType type, String rawSecret,
                            Set<String> auds, boolean scopeEnabled, Set<String> scopes, int accessTokenTtlMinutes,
                            boolean refreshTokenEnabled, int refreshTokenTtlMinutes, boolean sendTokenInCookie,
                            int cookieMaxAgeSeconds) {
        registeredClientRepository.findByClientName(clientName)
                                  .orElseGet(() -> registeredClientRepository.save(RegisteredClient.builder()
                                                                                                   .clientId(clientId)
                                                                                                   .clientName(
                                                                                                           clientName)
                                                                                                   .clientSecret(
                                                                                                           rawSecret ==
                                                                                                           null ? null
                                                                                                                :
                                                                                                           new BCryptPasswordEncoder().encode(
                                                                                                                        rawSecret))
                                                                                                   .clientType(type)
                                                                                                   .auds(auds)
                                                                                                   .scopeEnabled(
                                                                                                           scopeEnabled)
                                                                                                   .scopes(scopes)
                                                                                                   .accessTokenTtlMinutes(
                                                                                                           accessTokenTtlMinutes)
                                                                                                   .refreshTokenEnabled(
                                                                                                           refreshTokenEnabled)
                                                                                                   .refreshTokenTtlMinutes(
                                                                                                           refreshTokenTtlMinutes)
                                                                                                   .sendTokenInCookie(
                                                                                                           sendTokenInCookie)
                                                                                                   .cookieMaxAgeSeconds(
                                                                                                           cookieMaxAgeSeconds)
                                                                                                   .createdDate(
                                                                                                           Instant.now())
                                                                                                   .build()));
    }

    private void seedUsers() {
        User user = User.builder()
                        .email("user@user.com")
                        .activated(true)
                        .password(new BCryptPasswordEncoder().encode("a.1234"))
                        .name("John")
                        .surname("Doe")
                        .authProvider(AuthProviderType.KURTUBA)
                        .createdDate(Instant.now())
                        .build();
        userService.saveUser(user);
        UserSetting userSetting = UserSetting.builder()
                                             .user(user)
                                             .locale(localizationAvailableLocaleRepository.findByLanguageCodeAndCountryCode("en", "us").orElseThrow())
                                             .createdDate(Instant.now())
                                             .build();
        ;
        userSettingRepository.save(userSetting);
    }
}
