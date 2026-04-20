package com.kurtuba;

import com.kurtuba.auth.data.dto.LocalizationMessageDto;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.LocalizationSupportedCountry;
import com.kurtuba.auth.data.model.LocalizationSupportedLang;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.model.Role;
import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.data.repository.RoleRepository;
import com.kurtuba.auth.service.LocalizationMessageService;
import com.kurtuba.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("local")
public class BootstrapCLR implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private LocalizationMessageService localizationMessageService;

    @Autowired
    private LocalizationSupportedCountryRepository localizationSupportedCountryRepository;

    @Autowired
    private LocalizationSupportedLangRepository localizationSupportedLangRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedLocales();
        seedLocalizationMessages();
        seedRoles();
        seedRegisteredClients();
    }

    private void seedLocales() {
        seedLanguage("en");
        seedLanguage("tr");
        seedCountry("tr");
        seedCountry("us");
    }

    private void seedLanguage(String languageCode) {
        localizationSupportedLangRepository.findByLanguageCode(languageCode)
                .orElseGet(() -> localizationSupportedLangRepository.save(LocalizationSupportedLang.builder()
                        .languageCode(languageCode)
                        .createdDate(Instant.now())
                        .build()));
    }

    private void seedCountry(String countryCode) {
        localizationSupportedCountryRepository.findByCountryCode(countryCode)
                .orElseGet(() -> localizationSupportedCountryRepository.save(LocalizationSupportedCountry.builder()
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
                Map.entry("mail.account.modification.content.get-in-touch", "Get in touch")
        );

        messages.forEach((key, value) -> localizationMessageService
                .findByLanguageCodeAndKeyAndReturnOptional("en", key)
                .orElseGet(() -> localizationMessageService.create(LocalizationMessageDto.builder()
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
        roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

    private void seedRegisteredClients() {
        seedClient("default-client", RegisteredClientType.DEFAULT, null, Set.of("http://localhost:8080"),
                false, null, 60, true, 10080, false, false, false, 0);
        seedClient("demo-web-client", RegisteredClientType.WEB, null, Set.of("http://localhost:8080"),
                true, Set.of(AuthoritiesType.USER.name()), 60, true, 10080, true, true, false, 86400);
        seedClient("demo-service-client", RegisteredClientType.SERVICE, "demo-service-secret",
                Set.of("http://localhost:8080"), true, Set.of(AuthoritiesType.SERVICE.name()),
                60, false, 0, false, false, false, 0);
    }

    private void seedClient(String clientName, RegisteredClientType type, String rawSecret, Set<String> auds,
                            boolean scopeEnabled, Set<String> scopes, int accessTokenTtlMinutes,
                            boolean refreshTokenEnabled, int refreshTokenTtlMinutes,
                            boolean sendTokenInCookie, boolean cookieHttpOnly, boolean cookieSecure,
                            int cookieMaxAgeSeconds) {
        registeredClientRepository.findByClientName(clientName).orElseGet(() -> registeredClientRepository.save(
                RegisteredClient.builder()
                        .clientId(clientName)
                        .clientName(clientName)
                        .clientSecret(rawSecret == null ? null : new BCryptPasswordEncoder().encode(rawSecret))
                        .clientType(type)
                        .auds(auds)
                        .scopeEnabled(scopeEnabled)
                        .scopes(scopes)
                        .accessTokenTtlMinutes(accessTokenTtlMinutes)
                        .refreshTokenEnabled(refreshTokenEnabled)
                        .refreshTokenTtlMinutes(refreshTokenTtlMinutes)
                        .sendTokenInCookie(sendTokenInCookie)
                        .cookieHttpOnly(cookieHttpOnly)
                        .cookieSecure(cookieSecure)
                        .cookieMaxAgeSeconds(cookieMaxAgeSeconds)
                        .createdDate(Instant.now())
                        .build()));
    }

        /*localizationMessageService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("sms.account.activation.message")
                .message("Kurtuba activation code: ")
                .build());

        localizationMessageService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("sms.account.activation.sender")
                .message("KURTUBA")
                .build());*/


        /*localizationAvailableLocaleRepository.save(
                LocalizationAvailableLocale.builder()
                        .languageCode("tr")
                        .countryCode("tr")
                        .createdDate(Instant.now())
                        .build());


        localizationAvailableLocaleRepository.save(
                LocalizationAvailableLocale.builder()
                        .languageCode("en")
                        .countryCode("tr")
                        .createdDate(Instant.now())
                        .build());

        localizationAvailableLocaleRepository.save(
                LocalizationAvailableLocale.builder()
                        .languageCode("en")
                        .countryCode("us")
                        .createdDate(Instant.now())
                        .build());

        localizationAvailableLocaleRepository.save(
                LocalizationAvailableLocale.builder()
                        .languageCode("en")
                        .countryCode("uk")
                        .createdDate(Instant.now())
                        .build());*/


        /*localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.subject")
                .message("Kurtuba Account Activation")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.content.title")
                .message("THANKS FOR SIGNING UP!")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.content.greet")
                .message("Hi")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.content.code.prologue")
                .message("You're almost ready to get started. Here is your activation code")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.content.code.epilogue")
                .message("You can login to Kurtuba with your existing credentials to enter the code")
                .build());



        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.content.link.prologue")
                .message("You're almost ready to get started. Click below to activate your account")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.content.link.button.label")
                .message("ACTIVATE ACCOUNT")
                .build());


        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.content.closing")
                .message("Cheers")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.content.closing.subject")
                .message("Kurtuba Team")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.activation.content.get-in-touch")
                .message("Get In Touch")
                .build());





        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.password.reset.subject")
                .message("Kurtuba Password Reset")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.password.reset.content.code.prologue")
                .message("We received a request to reset your Kurtuba password. Here is you code")
                .build());


        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.password.reset.content.link.prologue")
                .message("We received a request to reset your Kurtuba password. Click the link below to reset your password")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.password.reset.content.epilogue")
                .message("If you didn't request to reset your password, ignore this email")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.password.reset.content.closing")
                .message("Cheers")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.password.reset.content.closing.subject")
                .message("Kurtuba Team")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.password.reset.content.get-in-touch")
                .message("Get In Touch")
                .build());



        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.email.verification.subject")
                .message("Kurtuba Email Verification")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.email.verification.content.title")
                .message("Verify Your E-mail Address!")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.email.verification.content.greet")
                .message("Hi")
                .build());


        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.email.verification.content.code.prologue")
                .message("Here is your verification code")
                .build());


        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.email.verification.content.link.prologue")
                .message("Click below to verify your email address")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.email.verification.content.link.button.label")
                .message("VERIFY YOUR EMAIL")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.email.verification.content.closing")
                .message("Cheers")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.email.verification.content.closing.subject")
                .message("Kurtuba Team")
                .build());


        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.email.verification.content.get-in-touch")
                .message("Get In Touch")
                .build());


        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.modification.subject")
                .message("Kurtuba Account Modification")
                .build());


        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.modification.content.greet")
                .message("Hi")
                .build());


        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.modification.content.prologue")
                .message("Your Kurtuba account metaName has changed.")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.modification.content.context")
                .message("Remember to use your new metaName the next time you want to log in to Kurtuba.")
                .build());


        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.modification.content.epilogue")
                .message("If you didn’t make this change, please get in touch straight away.")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.modification.content.closing")
                .message("Cheers")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.modification.content.closing.subject")
                .message("Kurtuba Team")
                .build());

        localizationService.create(LocalizationMessageDto.builder()
                .languageCode("en")
                .key("mail.account.modification.content.get-in-touch")
                .message("Get In Touch")
                .build());*/



        /*registeredClientRepository.save(RegisteredClient.builder()
                .clientId(UUID.randomUUID().toString())
                .clientName("default-client")
                .clientType(RegisteredClientType.DEFAULT)
                .scopeEnabled(false)
                .accessTokenTtlMinutes(5)
                .refreshTokenEnabled(true)
                .refreshTokenTtlMinutes(129600)
                .sendTokenInCookie(false)
                .createdDate(Instant.now())
                .build());*/

        /*registeredClientRepository.save(RegisteredClient.builder()
                .clientId(UUID.randomUUID().toString())
                .clientName("kurtuba-mobile-client")
                .clientType(RegisteredClientType.MOBILE)
                .auds(Set.of("https://app.kurtuba.tr"))
                .scopeEnabled(false)
                .accessTokenTtlMinutes(500)
                .refreshTokenEnabled(true)
                .refreshTokenTtlMinutes(129600)
                .sendTokenInCookie(false)
                .createdDate(Instant.now())
                .build());

        registeredClientRepository.save(RegisteredClient.builder()
                .clientId(UUID.randomUUID().toString())
                .clientName("adm-web-client")
                .clientSecret("$2a$12$vUM7IpBs2wk/0U21HmF5xeiumBgD8bJaBJ8OOox8TLAXC5erm4/Oq")
                .clientType(RegisteredClientType.WEB)
                .auds(Set.of("https://kurtuba.tr","https://adm.kurtuba.tr"))
                .scopeEnabled(true)
                .scopes(Set.of("ADMIN","USER","TEST"))
                .accessTokenTtlMinutes(300)
                .refreshTokenEnabled(true)
                .refreshTokenTtlMinutes(500)
                .sendTokenInCookie(true)
                .cookieMaxAgeSeconds(7776000)
                .createdDate(Instant.now())
                .build());
        registeredClientRepository.save(RegisteredClient.builder()
                .clientId(UUID.randomUUID().toString())
                .clientName("kurtuba-web-client")
                .clientType(RegisteredClientType.WEB)
                .auds(Set.of("https://kurtuba.tr"))
                .scopeEnabled(true)
                .scopes(Set.of("USER","TEST"))
                .accessTokenTtlMinutes(300)
                .refreshTokenEnabled(true)
                .refreshTokenTtlMinutes(129600)
                .sendTokenInCookie(true)
                .cookieMaxAgeSeconds(7776000)
                .createdDate(Instant.now())
                .build());
        registeredClientRepository.save(RegisteredClient.builder()
                .clientId(UUID.randomUUID().toString())
                .clientName("adm-service-client")
                .auds(Set.of("https://adm.kurtuba.tr"))
                .clientSecret("$2a$10$9u/FK6u3hyYv6LfiahN.ceeKhyToICqCvxOcJgH11EzB2YcKInwta")
                .clientType(RegisteredClientType.SERVICE)
                .scopeEnabled(true)
                .scopes(Set.of("SERVICE"))
                .accessTokenTtlMinutes(100)
                .refreshTokenEnabled(false)
                .sendTokenInCookie(false)
                .createdDate(Instant.now())
                .build());*/
        /*User user = new User();
        user.setEmail("user@user.com");
        user.setActivated(true);
        user.setPassword(new BCryptPasswordEncoder().encode("pass"));
        user.setName("John");
        user.setSurname("Doe");
        user.setAuthProvider(AuthProvider.KURTUBA);
        user.setDateCreated(Instant.now());
        userService.saveUser(user);
        user = new User();
        user.setEmail("user1@user.com");
        user.setActivated(true);
        user.setPassword(new BCryptPasswordEncoder().encode("pass"));
        user.setName("John");
        user.setSurname("Doe");
        user.setAuthProvider(AuthProvider.KURTUBA);
        user.setDateCreated(Instant.now());
        userService.saveUser(user);*/

}
