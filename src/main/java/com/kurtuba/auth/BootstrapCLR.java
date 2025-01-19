package com.kurtuba.auth;

import com.kurtuba.auth.data.dto.LocalizationMessageDto;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.LocalizationAvailableLocaleRepository;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.service.LocalizationMessageService;
import com.kurtuba.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class BootstrapCLR implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private LocalizationMessageService localizationMessageService;

    @Autowired
    private LocalizationAvailableLocaleRepository localizationAvailableLocaleRepository;

    @Override
    public void run(String... args) {


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
                        .createdDate(LocalDateTime.now())
                        .build());


        localizationAvailableLocaleRepository.save(
                LocalizationAvailableLocale.builder()
                        .languageCode("en")
                        .countryCode("tr")
                        .createdDate(LocalDateTime.now())
                        .build());

        localizationAvailableLocaleRepository.save(
                LocalizationAvailableLocale.builder()
                        .languageCode("en")
                        .countryCode("us")
                        .createdDate(LocalDateTime.now())
                        .build());

        localizationAvailableLocaleRepository.save(
                LocalizationAvailableLocale.builder()
                        .languageCode("en")
                        .countryCode("uk")
                        .createdDate(LocalDateTime.now())
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
                .createdDate(LocalDateTime.now())
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
                .createdDate(LocalDateTime.now())
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
                .createdDate(LocalDateTime.now())
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
                .createdDate(LocalDateTime.now())
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
                .createdDate(LocalDateTime.now())
                .build());*/
        /*User user = new User();
        user.setEmail("user@user.com");
        user.setActivated(true);
        user.setPassword(new BCryptPasswordEncoder().encode("pass"));
        user.setName("John");
        user.setSurname("Doe");
        user.setAuthProvider(AuthProvider.KURTUBA);
        user.setDateCreated(LocalDateTime.now());
        userService.saveUser(user);
        user = new User();
        user.setEmail("user1@user.com");
        user.setActivated(true);
        user.setPassword(new BCryptPasswordEncoder().encode("pass"));
        user.setName("John");
        user.setSurname("Doe");
        user.setAuthProvider(AuthProvider.KURTUBA);
        user.setDateCreated(LocalDateTime.now());
        userService.saveUser(user);*/

    }
}
