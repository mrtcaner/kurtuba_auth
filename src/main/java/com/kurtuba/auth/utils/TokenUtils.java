package com.parafusion.auth.utils;

import com.parafusion.auth.data.model.ClientType;
import io.jsonwebtoken.Jwts;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.keys.PbkdfKey;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@Component
public class TokenUtils {

    @Value("${parafusion.rsa-jwk.key}")
    private String rsaJwkKey;
    @Value("classpath:rsa-jwk")
    Resource rsaJwkFile;
    @Value("${auth.server.issuer-url}")
    private String authServerIssuerUrl;

    private PublicJsonWebKey publicJsonWebKey;

    //todo: Add user device id to the token. User's each device must get a seperate token. Tokens cannot be shared among devices.
    public String generateToken(String userId, ClientType clientType) {
        if (publicJsonWebKey == null) {
            //System.out.println("key null!");
            publicJsonWebKey = decrypJwk();
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR,5);
        return Jwts.builder()
                .setHeader(Map.of("kid",publicJsonWebKey.getKeyId()))
                .setIssuer(authServerIssuerUrl)
                .setSubject(userId)
                .setAudience(clientType.getClientTypeName())
                .setIssuedAt(new Date())
                .setNotBefore(new Date())
                .setExpiration(cal.getTime())
                .signWith(publicJsonWebKey.getPrivateKey())
                .compact();
    }

    private PublicJsonWebKey decrypJwk() {
        try {
            String jwkEncrypted = rsaJwkFile.getContentAsString(StandardCharsets.UTF_8);
            JsonWebEncryption decryptingJwe = new JsonWebEncryption();
            decryptingJwe.setCompactSerialization(jwkEncrypted);
            decryptingJwe.setKey(new PbkdfKey(rsaJwkKey));
            String payload = decryptingJwe.getPayload();

            return PublicJsonWebKey.Factory.newPublicJwk(payload);

        } catch (IOException | JoseException e) {
            throw new RuntimeException(e);
        }

    }

}
