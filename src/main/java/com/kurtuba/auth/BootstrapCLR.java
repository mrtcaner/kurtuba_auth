package com.parafusion.auth;

import com.parafusion.auth.data.model.AuthProvider;
import com.parafusion.auth.data.model.User;
import com.parafusion.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

@Component
public class BootstrapCLR implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) {
        /*User user = new User();
        user.setEmail("user@user.com");
        user.setActivated(true);
        user.setPassword(new BCryptPasswordEncoder().encode("pass"));
        user.setName("John");
        user.setSurname("Doe");
        user.setAuthProvider(AuthProvider.PARAFUSION);
        user.setDateCreated(LocalDateTime.now());
        userService.saveUser(user);
        user = new User();
        user.setEmail("user1@user.com");
        user.setActivated(true);
        user.setPassword(new BCryptPasswordEncoder().encode("pass"));
        user.setName("John");
        user.setSurname("Doe");
        user.setAuthProvider(AuthProvider.PARAFUSION);
        user.setDateCreated(LocalDateTime.now());
        userService.saveUser(user);*/

    }
}
