package com.kurtuba.auth.config;


import com.kurtuba.auth.config.provider.CustomAuthenticationProvider;
import com.kurtuba.auth.config.resolver.CustomBearerTokenResolver;
import com.kurtuba.auth.data.enums.RateLimitPublicApi;
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
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class DefaultSecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;


    private final CustomAuthenticationProvider authProvider;
    private final TokenUtils tokenUtils;
    private final Environment environment;
    private final RateLimitProperties rateLimitProperties;

    public DefaultSecurityConfig(CustomAuthenticationProvider authProvider, TokenUtils tokenUtils, Environment environment,
                                 RateLimitProperties rateLimitProperties) {
        this.authProvider = authProvider;
        this.tokenUtils = tokenUtils;
        this.environment = environment;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(
                AuthenticationManagerBuilder.class);
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
        jwtDecoderBuilder.jwsAlgorithms(signatureAlgorithms -> signatureAlgorithms.addAll(
                Set.of(SignatureAlgorithm.ES256, SignatureAlgorithm.RS256)));
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
     *
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    @Order(0)
    public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(publicSecurityMatchers())
            .authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable);
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
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(conf -> conf.maximumSessions(1))
            .authorizeHttpRequests(
                    authorize -> authorize.requestMatchers("/auth/login", "/auth/adm/login", "/error").permitAll().anyRequest().authenticated())
            .oauth2ResourceServer(
                    oauth2 -> oauth2.jwt(Customizer.withDefaults()))//will require token for certain endpoints
            .csrf(csrfConf -> csrfConf.disable())
            .logout(logout -> logout.logoutUrl("/auth/logout"));

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

    private String[] publicSecurityMatchers() {
        Stream<String> rateLimitedPublicApiMatchers = Arrays.stream(RateLimitPublicApi.values())
                                                 .map(rateLimitProperties::getPublicApi)
                                                 .map(RateLimitProperties.PublicApiProperties::getPattern);
        Stream<String> baseMatchers = Stream.of("/auth/oauth2/jwks", "/auth/adm/login", "/auth/service/login",
                                                "/v3/api-docs", "/v3/api-docs/**",
                                                "/swagger-ui.html", "/swagger-ui/**", "/webjars/**",
                                                "/favicon.ico", "/error");
        Stream<String> actuatorMatchers = isNonProdProfileActive()
                ? Stream.of("/actuator/**")
                : Stream.of("/actuator/health", "/actuator/health/**", "/actuator/info");

        return Stream.of(baseMatchers, actuatorMatchers, rateLimitedPublicApiMatchers)
                .flatMap(stream -> stream)
                .toArray(String[]::new);
    }

    private boolean isNonProdProfileActive() {
        return environment.acceptsProfiles(Profiles.of("local", "dev", "test"));
    }

}
