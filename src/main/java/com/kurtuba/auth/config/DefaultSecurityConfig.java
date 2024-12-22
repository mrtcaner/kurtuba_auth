package com.kurtuba.auth.config;


import com.kurtuba.auth.config.provider.CustomAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class DefaultSecurityConfig {

    private final CustomAuthenticationProvider authProvider;

    public DefaultSecurityConfig(CustomAuthenticationProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authProvider);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * A security filter to allow web clients to be able to call public endpoints with expired tokens
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    @Order(0)
    public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        http .securityMatcher("/auth/**", "/user/password/reset/**","/actuator/**", "/favicon.ico","/v3/api-docs")
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll())
                .csrf(csrfConf -> csrfConf.disable());
        return http.build();
    }

    /**
     *
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http.sessionManagement(conf->conf.maximumSessions(1))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("login", "/error").permitAll()
                        .requestMatchers("/.well-known/openid-configuration").permitAll() //If openid is not enabled,
                        // a call to this end point must return a 4xx. Otherwise, resource servers receive a login page as response and cannot get jwks
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))//will require token for certain endpoints
                .csrf(csrfConf -> csrfConf.disable())
                // Form login handles the redirect to the login page from the
                // authorization server filter chain
                .formLogin(Customizer.withDefaults());

        return http.build();
    }


}


