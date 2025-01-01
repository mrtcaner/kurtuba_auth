package com.kurtuba.auth;

import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class BootstrapCLR implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Override
    public void run(String... args) {
        /*
        registeredClientRepository.save(RegisteredClient.builder()
                .clientId(UUID.randomUUID().toString())
                .clientName("default-client")
                .clientType(RegisteredClientType.DEFAULT)
                .scopeEnabled(false)
                .accessTokenTtlMinutes(5)
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
                .scopeEnabled(true)
                .scopes(List.of("ADMIN","USER","TEST"))
                .accessTokenTtlMinutes(3)
                .refreshTokenEnabled(true)
                .refreshTokenTtlMinutes(5)
                .sendTokenInCookie(true)
                .cookieMaxAgeSeconds(7776000)
                .createdDate(LocalDateTime.now())
                .build());
        registeredClientRepository.save(RegisteredClient.builder()
                .clientId(UUID.randomUUID().toString())
                .clientName("kurtuba-web-client")
                .clientType(RegisteredClientType.WEB)
                .scopeEnabled(true)
                .scopes(List.of("ADMIN","USER","TEST"))
                .accessTokenTtlMinutes(3)
                .refreshTokenEnabled(true)
                .refreshTokenTtlMinutes(129600)
                .sendTokenInCookie(true)
                .cookieMaxAgeSeconds(7776000)
                .createdDate(LocalDateTime.now())
                .build());
        registeredClientRepository.save(RegisteredClient.builder()
                .clientId(UUID.randomUUID().toString())
                .clientName("adm-service-client")
                .clientSecret("$2a$10$9u/FK6u3hyYv6LfiahN.ceeKhyToICqCvxOcJgH11EzB2YcKInwta")
                .clientType(RegisteredClientType.SERVICE)
                .scopeEnabled(true)
                .scopes(List.of("SERVICE"))
                .accessTokenTtlMinutes(1)
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
