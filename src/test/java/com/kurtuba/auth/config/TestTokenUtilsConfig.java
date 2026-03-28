package com.kurtuba.auth.config;

import com.kurtuba.auth.utils.TokenUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
@Profile("test")
public class TestTokenUtilsConfig {

    @Bean("tokenUtils")
    @Primary
    TokenUtils tokenUtils() throws Exception {
        PublicJsonWebKey jwk = RsaJwkGenerator.generateJwk(2048);
        jwk.setKeyId("test-key");

        return new TokenUtils() {
            @Override
            public void init() {
                // Skip production JWK file decryption in tests.
            }

            @Override
            public List<PublicJsonWebKey> getAllSigningKeys() {
                return List.of(jwk);
            }

            @Override
            public String generateToken(String userId, Set<String> auds, Set<String> scopes, Duration duration, String clientId) {
                JwtBuilder builder = Jwts.builder()
                        .header()
                        .keyId(jwk.getKeyId())
                        .and()
                        .id(UUID.randomUUID().toString())
                        .issuer("http://localhost:8080")
                        .subject(userId)
                        .audience()
                        .add(auds)
                        .and()
                        .issuedAt(Date.from(Instant.now()))
                        .notBefore(Date.from(Instant.now()))
                        .expiration(Date.from(Instant.now().plus(duration)));

                if (scopes != null && !scopes.isEmpty()) {
                    builder = builder.claim("scope", scopes);
                }

                return builder
                        .signWith(jwk.getPrivateKey())
                        .compact();
            }

            @Override
            public String generateRefreshToken() {
                return Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
            }

            @Override
            public Claims getVerifiedTokenClaims(String token, long clockSkew) {
                PublicKey publicKey = jwk.getPublicKey();
                return Jwts.parser()
                        .clockSkewSeconds(clockSkew)
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            }
        };
    }
}
