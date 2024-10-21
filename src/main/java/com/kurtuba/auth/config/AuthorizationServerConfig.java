package com.parafusion.auth.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.parafusion.auth.data.model.CustomOAuth2User;
import com.parafusion.auth.service.UserService;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.keys.PbkdfKey;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.util.*;

@Configuration
public class AuthorizationServerConfig {

    @Value( "${parafusion.rsa-jwk.key}" )
    private String rsaJwkKey;
    @Value("classpath:rsa-jwk")
    Resource rsaJwkFile;
    @Value("${auth.server.issuer-url}")
    private String authServerIssuerUrl;
    @Value("${auth.server.mobile-client-secret}")
    private String mobileClientSecret;
    @Value("${auth.server.server-client-secret}")
    private String serverClientSecret;

    @Autowired
    private UserService userService;

    RegisteredClient mobileClient;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        return http.formLogin(Customizer.withDefaults()).build();//.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt) to add resource server capability
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        List<RegisteredClient> clientList = new ArrayList<>();
        mobileClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("mobile-client")
                .clientSecret(mobileClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .tokenSettings(tokenSettings())
                .redirectUri("http://localhost:8080/login/oauth2/code/articles-client-oidc")
                .redirectUri("http://localhost:8080/authorized")
                .redirectUri("http://localhost:8081/products")
                .redirectUri("parafusion-callback:/")
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                .build();
        clientList.add(mobileClient);

        RegisteredClient serverClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("server-client")
                .clientSecret(serverClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        clientList.add(serverClient);

        return new InMemoryRegisteredClientRepository(clientList);
    }

    @Bean
    public TokenSettings tokenSettings(){
        return TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .accessTokenTimeToLive(Duration.ofDays(10000))
                .refreshTokenTimeToLive(Duration.ofDays(10000))
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(){
        return new NimbusJwtEncoder(jwkSource());
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() throws JOSEException {
        JwtEncoder jwtEncoder = jwtEncoder();
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(jwtCustomizer());
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        accessTokenGenerator.setAccessTokenCustomizer(accessTokenCustomizer());
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }

    @Bean
    public OAuth2TokenCustomizer<OAuth2TokenClaimsContext> accessTokenCustomizer() {
        //Works for OAuth2TokenFormat.REFERENCE
        return context -> {
            CustomOAuth2User oauthUser = new CustomOAuth2User((OAuth2User) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal());
            OAuth2TokenClaimsSet.Builder claims = context.getClaims();
            claims.claim("sub", userService.getUserByUsernameOrEmail(oauthUser.getEmail()).getId());
        };
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        //Works for OAuth2TokenFormat.SELF_CONTAINED
        return context -> {
            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {

                context.getClaims().claim("sub", userService.getUserByUsernameOrEmail(context.getClaims().build().getClaim("sub")).getId());
            }
        };
    }


    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        try {
            String jwkEncrypted = rsaJwkFile.getContentAsString(StandardCharsets.UTF_8);
            JsonWebEncryption decryptingJwe = new JsonWebEncryption();
            decryptingJwe.setCompactSerialization(jwkEncrypted);
            decryptingJwe.setKey(new PbkdfKey(rsaJwkKey));
            String payload = decryptingJwe.getPayload();
            //PublicJsonWebKey publicJsonWebKey = PublicJsonWebKey.Factory.newPublicJwk(payload);
            // share the public part with whomever/whatever needs to verify the signatures
            JWKSet jwkSet = JWKSet.parse("{'keys':[" + payload + "]}");
            return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
        } catch (IOException | JoseException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /*@Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);

        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }*/

    /*private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }*/

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(authServerIssuerUrl)
                .build();
    }






}
