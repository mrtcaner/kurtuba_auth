package com.kurtuba.auth.config;

import com.kurtuba.auth.data.model.*;
import com.kurtuba.auth.data.repository.UserTokenRepository;
import com.kurtuba.auth.service.UserService;
import com.kurtuba.auth.utils.TokenUtils;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.shaded.gson.JsonObject;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.keys.PbkdfKey;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AccessTokenResponseAuthenticationSuccessHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Value("classpath:rsa-jwk")
    Resource rsaJwkFile;
    @Value("${kurtuba.rsa-jwk.key}")
    private String rsaJwkKey;
    @Value("${auth.server.issuer-url}")
    private String authServerIssuerUrl;
    @Value("${auth.server.mobile-client-secret}")
    private String mobileClientSecret;
    @Value("${auth.server.adm-web-client-secret}")
    private String admWebClientSecret;
    @Value("${auth.server.adm-service-client-secret}")
    private String admServiceClientSecret;

    private final UserService userService;

    private final UserTokenRepository userTokenRepository;

    private final SessionFactory sessionFactory;

    public AuthorizationServerConfig(UserService userService, UserTokenRepository userTokenRepository, SessionFactory sessionFactory) {
        this.userService = userService;
        this.userTokenRepository = userTokenRepository;
        this.sessionFactory = sessionFactory;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = http.getConfigurer(OAuth2AuthorizationServerConfigurer.class);

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) -> {
                    /*authorizationServer
                            .oidc(Customizer.withDefaults());	// Enable OpenID Connect 1.0*/
                            authorizationServer.authorizationServerSettings(AuthorizationServerSettings.builder().issuer(authServerIssuerUrl).build());
                            authorizationServer.tokenEndpoint(oAuth2TokenEndpointConfigurer -> oAuth2TokenEndpointConfigurer
                                    .accessTokenResponseHandler((request, response, authentication) -> {

                                        JsonObject tokenObj = TokenUtils.decodeTokenPayload(((OAuth2AccessTokenAuthenticationToken) authentication).getAccessToken().getTokenValue());
                                        /*
                                            "sub" -> {JsonPrimitive@24061} ""3f29802a-64fa-41e0-be86-82f3c24ea982""
                                            "aud" -> {JsonPrimitive@24063} ""mobile-client""
                                            "nbf" -> {JsonPrimitive@24065} "1732304516"
                                            "iss" -> {JsonPrimitive@24067} ""http://localhost:8080""
                                            "exp" -> {JsonPrimitive@24069} "2596304516"
                                            "iat" -> {JsonPrimitive@24071} "1732304516"
                                            "jti" -> {JsonPrimitive@24073} ""73d69d7e-dfc5-45b0-9130-975e5644e0d9""
                                         */
                                        // no need to save short-lived service-client tokens
                                        if(!tokenObj.get(JWTClaimsEnum.SUB.getDisplayName()).getAsString().contains("service-client")) {


                                            Instant instant = Instant.ofEpochSecond(Long.parseLong(tokenObj.get(JWTClaimsEnum.EXP.getDisplayName()).getAsString()));
                                            ZoneId zoneId = ZoneId.systemDefault();
                                            LocalDateTime expirationDate = instant.atZone(zoneId).toLocalDateTime();
                                            //System.out.println("instant1:" + instant);
                                            instant = Instant.ofEpochSecond(Long.parseLong(tokenObj.get(JWTClaimsEnum.IAT.getDisplayName()).getAsString()));
                                            LocalDateTime issuedAt = instant.atZone(zoneId).toLocalDateTime();
                                            //System.out.println("instant2:" + instant);

                                            UserToken userToken = UserToken.builder()
                                                    .userId(tokenObj.get(JWTClaimsEnum.SUB.getDisplayName()).getAsString())
                                                    .clientId(tokenObj.get(JWTClaimsEnum.AUD.getDisplayName()).getAsString())
                                                    .jti(tokenObj.get(JWTClaimsEnum.JTI.getDisplayName()).getAsString())
                                                    .expirationDate(expirationDate)
                                                    .createdDate(issuedAt)
                                                    .build();

                                            Session session = sessionFactory.openSession();
                                            session.beginTransaction();
                                            userTokenRepository.save(userToken);
                                            session.getTransaction().commit();
                                            session.close();
                                        }
                                        //fill the response
                                        OAuth2AccessTokenResponseAuthenticationSuccessHandler handler = new OAuth2AccessTokenResponseAuthenticationSuccessHandler();
                                        handler.onAuthenticationSuccess(request, response, authentication);
                                    }));
                        }

                )
                // Redirect to the login page when not authenticated from the
                // authorization endpoint
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }


    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        List<RegisteredClient> clientList = new ArrayList<>();
        RegisteredClient mobileClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("mobile-client")
                .clientSecret(mobileClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .tokenSettings(tokenSettings())
                .redirectUri("http://localhost:8080/authorized")
                .redirectUri("http://localhost:8081/products")
                .redirectUri("parafusion-callback:/")
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                .build();
        clientList.add(mobileClient);

        RegisteredClient admWebClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("adm-web-client")
                .clientSecret(admWebClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .tokenSettings(tokenSettings())
                .redirectUri("http://localhost:8080/authorized")
                .redirectUri("http://localhost:8081/products")
                .redirectUri("parafusion-callback:/")
                .redirectUri("https://oauth.pstmn.io/v1/callback")
                .build();

        clientList.add(admWebClient);

        RegisteredClient admServiceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("adm-service-client")
                .clientSecret(admServiceClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        clientList.add(admServiceClient);

        return new InMemoryRegisteredClientRepository(clientList);
    }

    @Bean
    public TokenSettings tokenSettings() {
        return TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .accessTokenTimeToLive(Duration.ofDays(1825))//5 years
                //.refreshTokenTimeToLive(Duration.ofDays(10000))
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(jwkSource());
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() {
        JwtEncoder jwtEncoder = jwtEncoder();
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(jwtCustomizer());
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        accessTokenGenerator.setAccessTokenCustomizer(accessTokenCustomizer());
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }

    /**
     * Alternate token customizer
     * Works for OAuth2TokenFormat.REFERENCE
     * @return
     */
    @Bean
    public OAuth2TokenCustomizer<OAuth2TokenClaimsContext> accessTokenCustomizer() {
        return context -> {
            CustomOAuth2User oauthUser = new CustomOAuth2User((OAuth2User) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal());

            if(context.getAuthorizationGrantType().equals(AuthorizationGrantType.CLIENT_CREDENTIALS)){
                //a service is asking for an access token to call another service
                // make it a short-lived token
                context.getClaims().claim(JWTClaimsEnum.EXP.getDisplayName(), Instant.now().plus(Duration.ofMinutes(1)));
                context.getClaims().claim(JWTClaimsEnum.SCOPE.getDisplayName(), "SERVICE");
            }else{
                //a user is logging in. replace username/email with userId
                User user = userService.getUserByUsernameOrEmail(oauthUser.getEmail());
                context.getClaims().claim(JWTClaimsEnum.SUB.getDisplayName(), user.getId());

                List<String> roles =  user.getUserRoles().stream().map(userRole -> userRole.getRole().name()).toList();
                // use userId for sub
                context.getClaims().claim(JWTClaimsEnum.SUB.getDisplayName(), user.getId());

                if(roles.contains(AuthoritiesEnum.ADMIN.name()) && context.getRegisteredClient().getClientName()
                        .equals("adm-web-client")){
                    //if user has admin role and asks for a token for the web client then make his token short-lived
                    context.getClaims().claim(JWTClaimsEnum.EXP.getDisplayName(), Instant.now().plus(Duration.ofMinutes(3)));
                    context.getClaims().claim(JWTClaimsEnum.SCOPE.getDisplayName(), roles);
                }
            }
        };
    }

    /**
     *  Default token customizer
     *  Works for OAuth2TokenFormat.SELF_CONTAINED which is the default token setting
     * @return
     */
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        //
        return context -> {
            //Make sure it is not a refresh token
            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {

                if(context.getAuthorizationGrantType().equals(AuthorizationGrantType.CLIENT_CREDENTIALS)){
                    //a service is asking for an access token to call another service
                    // make it a short-lived token
                    context.getClaims().claim(JWTClaimsEnum.EXP.getDisplayName(), Instant.now().plus(Duration.ofMinutes(1)));
                    context.getClaims().claim(JWTClaimsEnum.SCOPE.getDisplayName(), AuthoritiesEnum.SERVICE.name());

                }else{
                    //a user is logging in. replace username/email with userId
                    User user = userService.getUserByUsernameOrEmail(context.getClaims().build().getClaim("sub"));
                    List<String> roles =  user.getUserRoles().stream().map(userRole -> userRole.getRole().name()).toList();
                    // use userId for sub
                    context.getClaims().claim(JWTClaimsEnum.SUB.getDisplayName(), user.getId());

                    if(roles.contains(AuthoritiesEnum.ADMIN.name()) && context.getRegisteredClient().getClientId()
                            .equals("adm-web-client")){
                        //if user has admin role and asks for a token for the web client then make his token short-lived
                        context.getClaims().claim(JWTClaimsEnum.EXP.getDisplayName(), Instant.now().plus(Duration.ofMinutes(3)));
                        context.getClaims().claim(JWTClaimsEnum.SCOPE.getDisplayName(), roles);
                    }

                }

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
            JWKSet jwkSet = JWKSet.parse("{\"keys\":[" + payload + "]}");
            return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
        } catch (IOException | JoseException | ParseException e) {
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


}
