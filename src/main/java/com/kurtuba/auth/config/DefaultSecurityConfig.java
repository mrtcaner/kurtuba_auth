package com.kurtuba.auth.config;


import com.kurtuba.auth.config.provider.CustomAuthenticationProvider;
import com.kurtuba.auth.config.resolver.CustomBearerTokenResolver;
import com.kurtuba.auth.utils.TokenUtils;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.JsonArray;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import org.jose4j.jwk.JsonWebKey;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.text.ParseException;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class DefaultSecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;


    private final CustomAuthenticationProvider authProvider;
    private final TokenUtils tokenUtils;

    public DefaultSecurityConfig(CustomAuthenticationProvider authProvider, TokenUtils tokenUtils) {
        this.authProvider = authProvider;
        this.tokenUtils = tokenUtils;
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

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder jwtDecoderBuilder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri);
        jwtDecoderBuilder.jwsAlgorithms(signatureAlgorithms ->
                signatureAlgorithms.addAll(Set.of(SignatureAlgorithm.ES256,SignatureAlgorithm.RS256)));
        JwtDecoder jwtDecoder = jwtDecoderBuilder.build();

        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) throws JwtException {
                return jwtDecoder.decode(token);
            }
        };
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
        http .securityMatcher("/auth/**", "/user/password/reset/**","/user/email/verification/link/**",
                        "/registration/**","/actuator/**", "/favicon.ico", "/v3/api-docs", "/oauth2/jwks",
                        "/sms/**", "/message-status/**")
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
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))//will require token for certain endpoints
                .csrf(csrfConf -> csrfConf.disable())
                // Form login handles the redirect to the login page from the
                // authorization server filter chain
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public JWKSet jwkSource() {
        try {
            JsonObject keysObj = new JsonObject();
            JsonArray keysArray = new JsonArray();
            tokenUtils.getAllSigningKeys().stream().forEach(key -> {
                keysArray.add(JsonParser.parseString(key.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY)));
            });
            keysObj.add("keys", keysArray);
            return JWKSet.parse(keysObj.toString());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return new CustomBearerTokenResolver();
    }


}


