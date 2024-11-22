package com.kurtuba.auth.utils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.MalformedURLException;
import java.net.URL;

public class JWKS {

    
    public static void main(String[] args) throws Exception {

        // The access token to validate, typically submitted with a HTTP header like
        String accessToken =
                "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ijg1MmI0Yzc0MTM1MWVlZGM2ZWM4YWU3ODNiOThlZWFiZTIxNTdkYWEifQ.eyJpc3MiOiJodHRwczpcL1wvd3d3LmZhY2Vib29rLmNvbSIsImF1ZCI6IjE3ODg1Nzc5ODA3Njc1NiIsInN1YiI6IjEwMTU5MjAxMTgwNTI3Nzk3IiwiaWF0IjoxNjg2ODMxMTU4LCJleHAiOjE2ODY4MzQ3NTgsImp0aSI6IlJ5VWMuNGM5ZWZlY2NjYTAyOWZjNjFmZDU5ZWUwZDBlODdmM2Q4ZTA0ZTgwNDU2MjUyMjE3OTZhNDBhODBiMDJkYjhiZiIsIm5vbmNlIjoiIiwiYXRfaGFzaCI6IlV1Q3V5eXNPQmsxRE1wSTY0TWppbEEiLCJlbWFpbCI6Im11cmF0Y2FuZXJcdTAwNDB5bWFpbC5jb20iLCJnaXZlbl9uYW1lIjoiTXVyYXQiLCJmYW1pbHlfbmFtZSI6IkNhbmVyIiwibmFtZSI6Ik11cmF0IENhbmVyIiwicGljdHVyZSI6Imh0dHBzOlwvXC9wbGF0Zm9ybS1sb29rYXNpZGUuZmJzYnguY29tXC9wbGF0Zm9ybVwvcHJvZmlsZXBpY1wvP2FzaWQ9MTAxNTkyMDExODA1Mjc3OTcmaGVpZ2h0PTEwMCZ3aWR0aD0xMDAmZXh0PTE2ODk0MjMxNTkmaGFzaD1BZVFVRk5PbC02eUo5cndKU2w0In0.tC1fqzT7E_gJlSAYq9PXJYs7bQ2R03RbB2jP1iVSZf8Hf51OuYSQSd1WzipxwRe-Y0NZQDch0S1khqV1lTMG6BrSU6O6tKbeeDHhWKab4D-c1Rhe_z5PmnBF0BPI5jiPXLtD7bGv6qd-v2xOVw2UNcGOEPwXKQlFkDQBmwVojZmiuo9Bq1yjAc2UtTBGYOZ_QUMvEUXWEXMuWQAtoy5BNHfBpOZUYbDp7233aSc2_onUqRa";

        // Set up a JWT processor to parse the tokens and then check their signature
        // and validity time window (bounded by the "iat", "nbf" and "exp" claims)
        ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();

        // The public RSA keys to validate the signatures will be sourced from the
        // OAuth 2.0 server's JWK set, published at a well-known URL. The RemoteJWKSet
        // object caches the retrieved keys to speed up subsequent look-ups and can
        // also gracefully handle key-rollover
        JWKSource keySource = null;
        try {

            keySource = new RemoteJWKSet(new URL("https://www.facebook.com/.well-known/oauth/openid/jwks"),
                    new DefaultResourceRetriever(12000, 12000));

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //    JWKSource keySource = new RemoteJWKSet(new URL("https://localhost:9443/oauth2/jwks"));

        // The expected JWS algorithm of the access tokens (agreed out-of-band)
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;

        // Configure the JWT processor with a key selector to feed matching public
        // RSA keys sourced from the JWK set URL
        JWSKeySelector keySelector = new JWSVerificationKeySelector(expectedJWSAlg, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
        // Process the token
        SecurityContext ctx = null; // optional context parameter, not required here
        JWTClaimsSet claimsSet = jwtProcessor.process(accessToken, ctx);

        // Print out the token claims set
        System.out.println(claimsSet.toJSONObject());
    }

}