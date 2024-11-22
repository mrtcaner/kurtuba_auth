package com.kurtuba.auth.config;


import com.kurtuba.auth.service.CustomOAuth2UserService;
import com.kurtuba.auth.service.UserDetailsServiceImpl;
import com.kurtuba.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class DefaultSecurityConfig {

    @Autowired
    private CustomOAuth2UserService oauthUserService;

    @Autowired
    private UserService userService;

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder, UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    /*@Bean
    @Order(1)
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/error", "/actuator/**", "/auth/login", "/register/**", "/mail/**", "/static/favicon.ico").permitAll()
                        .requestMatchers("/h2/**").permitAll()
                        .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())

                .formLogin(form -> form.loginPage("/login").permitAll().successHandler((request, response, authentication) -> {
                    System.out.println("Form login successful!");
                }).defaultSuccessUrl("/"))

                .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(Customizer.withDefaults()).successHandler((request, response, authentication) -> {
                    System.out.println("Social login successful!");
                    //Runs when sign in with Google
                    CustomOAuth2User oauthUser = new CustomOAuth2User((OAuth2User) authentication.getPrincipal());
                    userService.processOAuthPostLogin(oauthUser.getEmail());
                }).defaultSuccessUrl("/"))
                .userDetailsService(userDetailsService())
                .logout(httpSecurityLogoutConfigurer -> httpSecurityLogoutConfigurer.logoutSuccessUrl("/"))
                .userDetailsService(userDetailsService())
                .exceptionHandling(exHandling -> exHandling.authenticationEntryPoint(new CustomAuthenticationEntryPoint()))
                .build();
    }*/

    /*@Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                ).exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .formLogin(withDefaults());
        return http.build();
    }*/

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("login", "/error", "/actuator/**", "/auth/**", "/favicon.ico").permitAll()
                        .requestMatchers("/h2/**").permitAll()
                        .anyRequest()
                        .authenticated()).csrf(csrf -> csrf.disable())
                // Form login handles the redirect to the login page from the
                // authorization server filter chain
                .formLogin(Customizer.withDefaults());

        return http.build();
    }


}
