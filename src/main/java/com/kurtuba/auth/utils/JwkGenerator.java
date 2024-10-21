package com.parafusion.auth.utils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.keys.PbkdfKey;
import org.jose4j.lang.JoseException;

import java.net.URISyntaxException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * 
 */
class JwkGenerator {


    public static void main(String args[]) throws JoseException, ParseException, JOSEException, URISyntaxException {
        //String keys = createJwk();
        //decrypJwk("eyJhbGciOiJQQkVTMi1IUzI1NitBMTI4S1ciLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwicDJjIjo2NTUzNiwicDJzIjoiWnpHZWp5NWpnbVBGcEpZNSJ9.q4toDIxT62HvPudSLxpE5FwqxX91H5uPLltngPz6V1rZGCj1Pi0JWw.VhRExHSCPY7gCjSYsmJUuA.NdbQtsPp3fdQYtM5nFGX5hNkOnFiBaAPcrO5lExxugWmX3_xyPL7lcRC3abBFQeRR9niwISqQFQx7LBgeQ5m_2A8w-SccRTRDVjO4acqjnM1qi6NyCmIn0za5VbjW9gmQb2V_-HilZE40MYPczMpjkhbfWxJyagMlLmWHamPN9HG_KqBjF6RiGkRZiOUqkOJ860rl7QBcps-CKpX_X-yHUU3tRD6HyXXtIw63DJ5W6MDHAmeVzVjuPCgZXDt7ZJT2rP6Wo5F0ajJF21fot2UF0lzpZk6RJknV99-5ESk91GfRJ_DEVbe9fK3ltopapvpjs1NPjifmkTI4XhGvIQhQGoG8CZQjJhazysx7A-mJc-kLpVVELocSpUpIAfiQYvytjG5qj2XU0ALQtaGPXma8u5r38LnDBJTTW-OrLJVa9KuYZOvmtCmkWDZ8FPs0R2LlZIGrR6oKgQSUZO-rDECxwxKDDl5rd50VqbkcLYS5SFcH5VW3nfHptSUfKTKGBB7l8jTV2jxmSYFvVByCarkpuGC-mGtS08IStHyGJRLCfK9DKdVJHCR7qRykDB8xZdTGlBTAakHGiuoMeDzc8clvAFuDXJiOKC4mcrwTbB2Iw7GnaSiSxJH3CvoO_84hBbkTHvgW76WebNJhtIMMTs7kVl3dZ0aESriztH_fZzc3HHFvRaOu-R0ilNH3zWvXivCAFR-l-zsjSMKhVm4SmS9x4r5hi1bHnRZ-JPcokEwO932h1AvSakq_8bZTKAE1vnrdLtGzlFFwiqfsGloT-PI8CExKRpolFyqO5isOIztP0O9bfwK2FZ9Wukdu9jk-Btn2_qp8xct6YzzGqir65Wv_qxFxz32B2KFGBDHF5njYWvOi7Z_8WOPM3BVRrlbIvZKrTWd386RPMWtrGknK1Kt7jDen_F8Ez0inrXPn5-TrUD5-ZfNb29l1TmllLB8j5XvsjdO1lsRE36pAgjGFpYBrt2tUcNRx3_-r26z-v2FcXsY6R5Jh9qtdxxmeFyrHPr8fSKGjXbgNFpydlnzbGEjvKdlBG9BGBBSsjNNbm5L_G5EZeJ7kZmlW2OT9AWH8KxceozbiTEpSERHbJu_GqUZCZKkcNIbp7soCuHqOhgwUA5uhVyoTeWQiAse93bL0AujEM3nioQzxEbpW1-JO5-u5GuPIgGuE3go1cjkc9ZaNLmDnBLH-ONcGIun4mfkcNM-zig-_GqZz7_AjkhBv25gaT9gSdQEC1IZ6dAAsiIWc2_9H2Xg_r0GyN9ud_tuvWfz3ifRq079b8cpiHth1JEGPcC5K42zR7B81dqnKlGZFS-MZnT10PtsAQXrTKoIyngdokQPrt8k_qRA46kNwF9Hi5KG9CvtuaqqabycRUXmBK11iicgKmhoLydhxAdx1TMxCH1vWtvTlCtjRo4mQQY5_zDnJ9jDS68iNsSSu6DeP-L5TvXTMTxzrt1O1CQQNqSjF8QImA8q_uNM5Z27VhICA-HYsuCnnl2E4k0LGajpaJ_yqPtgCtgcrhcyrg4MuUgEeQowZ6YAiEJ3xU3e3Z4cufNZpzhDaTS2UMZ8JzyI7gWYHUGQylHKOb05kbt9PFwHkzopP810LqcqsYtxF7EngLl5uDbpj4bPqiINdTFnigOsbrYanJeDMyCwQ-GvW07xN8UJUXOOiCXMNtyDnnHAhaDOL5iMWbpP7sxlSjYpVmL8HRUojGuXm-ZmFm6aof9hQwK-3icvUMy9OOn7YH8gQzvcDbvSWKtUDrql84P3xTJChIqy1DuHyRzoT8DfaNrEDJuPQjVgF0XkALPZ7pbGZl8a9M5QrdkvI_XWD6KFuqDIoi0M_4w3A04z4QJEBJjkYRoMJqtXFc4d5Ul1BAP37R_S6sGydK0IijhWcMbegf4CMMtgbN9OeYR7vqKLzx7ak3IE2JlPaY6apAxB2BWKuuXiKQVjuWSxzN6mr0tPJFCgP_8iCR2a3jNwjgD4oPL74iTQ5C9vrPSTCmnpQg83xqiN_dT7jlyPPM4BSxd5XCDh62dPMRciRfz_Sym9uvAQggRY0Pd0acg0YfH-xWLAnNoFGux8ozV2v4uNEbTx41jqxztk7iLw-q2I_0JyUG2hVG_oqbI6Fcz1yc0TIk30QnDkNRQ0LFC9qYj-ZxxpUL0OEiLxpgu1t7i6zuXY8Qgx2yMcZHsHqRGuV-KMNJuIdx2bxcOcD0YXZxQVYo1A72g.OqwG58TaL-1Qn5p9Ho-lNA");
        //rsaJwk();

        //System.out.println(new BCryptPasswordEncoder().encode("eaffc1de-1c95-4f62-8862-21f684e798fa"));
        //System.out.println(new BCryptPasswordEncoder().encode("ece3e093-dea9-4492-9a53-c509a2f700fd"));
    }

    public static void createJjwtToken(){
        String jws = Jwts.builder()
                .setIssuer("Stormpath")
                .setSubject("msilverman")
                .claim("name", "Micah Silverman")
                .claim("scope", "admins")
                // Fri Jun 24 2016 15:33:42 GMT-0400 (EDT)
                .setIssuedAt(Date.from(Instant.ofEpochSecond(1466796822L)))
                // Sat Jun 24 2116 15:33:42 GMT-0400 (EDT)
                .setExpiration(Date.from(Instant.ofEpochSecond(4622470422L)))
                .signWith(
                        SignatureAlgorithm.HS256,
                        TextCodec.BASE64.decode("Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=")
                )
                .compact();
    }

    public static void decrypJwk(String jweEncryptedJwk) throws JoseException, ParseException {
        JsonWebEncryption decryptingJwe = new JsonWebEncryption();
        decryptingJwe.setCompactSerialization(jweEncryptedJwk);
        decryptingJwe.setKey(new PbkdfKey("FD7pr&.f!my?k#8:"));
        String payload = decryptingJwe.getPayload();
        System.out.println("decrypted payload:" + payload);

        PublicJsonWebKey publicJsonWebKey = PublicJsonWebKey.Factory.newPublicJwk(payload);
        // share the public part with whomever/whatever needs to verify the signatures
        RSAPrivateKey pKey = (RSAPrivateKey) publicJsonWebKey.getPrivateKey();
        RSAPrivateCrtKey pCrtKey = (RSAPrivateCrtKey) publicJsonWebKey.getPrivateKey();
        System.out.println("pKey" + pKey.toString());
        System.out.println("pKeyCrt" + pCrtKey.toString());

        String jws = Jwts.builder()
                .setHeader(Map.of("kid",publicJsonWebKey.getKeyId()))
                .setIssuer("http://192.168.1.38:8080")
                .setSubject("f1c7e803-b443-47f9-83a1-a77dcff7be38")
                .setAudience("mobile-client")
                // Fri Jun 24 2016 15:33:42 GMT-0400 (EDT)
                .setIssuedAt(Date.from(Instant.ofEpochSecond(1466796822L)))
                .setNotBefore(Date.from(Instant.ofEpochSecond(1466796822L)))
                // Sat Jun 24 2116 15:33:42 GMT-0400 (EDT)
                .setExpiration(Date.from(Instant.ofEpochSecond(4622470422L)))
                .signWith(pKey)
                .compact();

        System.out.println("JWS:" + jws);

        System.out.println(publicJsonWebKey.getPrivateKey());
        System.out.println(publicJsonWebKey.getPublicKey());
        System.out.println("**********************");
        System.out.println(publicJsonWebKey.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
        System.out.println(publicJsonWebKey.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
        JWKSet set = JWKSet.parse("{'keys':[" + payload + "]}");

    }

    public static void rsaJwk() throws JOSEException {
        RSAKey jwk = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key (optional)
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID (optional)
                .generate();

// Output the private and public RSA JWK parameters
        System.out.println(jwk);

// Output the public RSA JWK parameters only
        System.out.println(jwk.toPublicJWK());
    }


    public static String createJwk() throws JoseException {
        JsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId(UUID.randomUUID().toString());
        String jwkjson = rsaJsonWebKey.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE);
        JsonWebEncryption encryptingJwe = new JsonWebEncryption();
        encryptingJwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.PBES2_HS256_A128KW);
        encryptingJwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        encryptingJwe.setKey(new PbkdfKey("password-goes-here"));
        encryptingJwe.setPayload(jwkjson);
        System.out.println("Encrypted Payload:" + jwkjson);
        String jweEncryptedJwk = encryptingJwe.getCompactSerialization();
        System.out.println("jweEncryptedJwk:" + jweEncryptedJwk);
        return jweEncryptedJwk;
    }

}