package com.kurtuba.auth.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kurtuba.auth.data.model.dto.TokenDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.netty.http.client.HttpClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML;

@Component
public class AutoLoginUtil {

    @Value("${auth.server.email-password-login-endpoint}")
    private String authServerEmailPasswordLoginUrl;

    private static final String OAUTH2_URI = "/oauth2/authorize?response_type=code&client_id=mobile-client&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fauthorized";

    /**
     *  Mimics user form login steps for authorization code
     *  Useful for testing purposes
     * @param email
     * @param pass
     * @return Access Token and refresh token
     */
    public String getOauth2AccessToken(String email, String pass){
        MultiValueMap<String, String> tempCookies = new LinkedMultiValueMap<String, String>();

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://localhost:8080");
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()))
                .uriBuilderFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .exchangeStrategies(ExchangeStrategies.builder().codecs(clientCodecConfigurer -> acceptedCodecs(clientCodecConfigurer)).build())
                .build();
        client.mutate().uriBuilderFactory(factory).build();

        StringBuilder redirectUrl = new StringBuilder();

        client
                .get()
                .uri(OAUTH2_URI)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .exchange()
                .flatMap(clientResponse -> {
                    //this must be a redirect to login
                    //Lets see if there is a cookie
                    System.out.println("redirect to login cookie:" + clientResponse.cookies().get("JSESSIONID"));
                    for (CharSequence key: clientResponse.cookies().keySet()) {
                        tempCookies.put(String.valueOf(key), Arrays.asList(clientResponse.cookies().get(key).stream().toList().get(0).getValue()));
                    }
                    if(clientResponse.statusCode().is3xxRedirection()){
                        redirectUrl.append(clientResponse.headers().header("location").get(0));
                        System.out.println("First redirect:" + redirectUrl);

                    }
                    return clientResponse.bodyToMono(String.class);
                }).block();

        //get Login page
        client.get().uri(redirectUrl.toString())
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .cookies(cookies -> cookies.addAll(tempCookies))
                .exchange().flatMap(clientResponse1 -> {
                    //must be login page, lets login!
                    System.out.println("get login cookie:" + clientResponse1.cookies().get("JSESSIONID"));
                    for (CharSequence key: clientResponse1.cookies().keySet()) {
                        tempCookies.put(String.valueOf(key), Arrays.asList(clientResponse1.cookies().get(key).stream().toList().get(0).getValue()));
                    }
                    return clientResponse1.bodyToMono(String.class);
                }).block();

        //authenticate user, results redirect to primary uri+continue
        client.post().uri("/login")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .header(HttpHeaders.REFERER, "http://localhost:8080" + OAUTH2_URI)
                .cookies(cookies -> cookies.addAll(tempCookies))
                .body(BodyInserters.fromFormData("username", email)
                        .with("password", pass))
                .exchange()
                .flatMap(clientResponse2 -> {
                    //another redirect to "uri"&continue!? Let's get the url and see!
                    System.out.println("post login cookie:" + clientResponse2.cookies().get("JSESSIONID"));
                    for (CharSequence key: clientResponse2.cookies().keySet()) {
                        tempCookies.put(String.valueOf(key), Arrays.asList(clientResponse2.cookies().get(key).stream().toList().get(0).getValue()));
                    }
                    if(clientResponse2.statusCode().is3xxRedirection()){
                        //Aww Yisss! Another redirect!
                        redirectUrl.setLength(0);
                        redirectUrl.append(clientResponse2.headers().header("location").get(0));

                    }
                    return clientResponse2.bodyToMono(String.class);
                }).block();

        //a redirect to uri + continue. will end with another redirect to code url but no need to call it.
        //we will just parse it
        StringBuilder authorization_code = new StringBuilder();
        client.get().uri(redirectUrl.toString())
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .cookies(cookies -> cookies.addAll(tempCookies))
                .exchange()
                .flatMap(clientResponse3 -> {
                    System.out.println("get continue cookie:" + clientResponse3.cookies().get("JSESSIONID"));
                    //this must be another redirect. This time a url with code=<auth-code> url param
                    if(clientResponse3.statusCode().is3xxRedirection()){
                        // we have an authorization_code now, Yay!
                        try {
                            Map<String, List<String>> re = splitQuery(new URL(clientResponse3.headers().header("location").get(0)));
                            System.out.println("code:" + re.get("code").get(0));
                            authorization_code.append(re.get("code").get(0));
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    return clientResponse3.bodyToMono(String.class);
                }).block();

        String username = "mobile-client";
        String password = "eaffc1de-1c95-4f62-8862-21f684e798fa";
        String basicAuth ="Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        MultiValueMap<String, String> bodyValues = new LinkedMultiValueMap<>();
        bodyValues.add("redirect_uri","http://localhost:8080/authorized");
        bodyValues.add("grant_type","authorization_code");
        bodyValues.add("code",authorization_code.toString());

        //authenticate user, results redirect to primary uri+continue
        String resp = client.post()
                .uri("/oauth2/token")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header(HttpHeaders.AUTHORIZATION, basicAuth)
                .cookies(cookies -> cookies.addAll(tempCookies))
                .body(BodyInserters.fromFormData(bodyValues))
                .exchange()
                .flatMap(clientResponse4 -> {
                    //another redirect to "uri"&continue!? Let's get the url and see!
                    System.out.println("post login cookie:" + clientResponse4.cookies().get("JSESSIONID"));
                    for (CharSequence key: clientResponse4.cookies().keySet()) {
                        tempCookies.put(String.valueOf(key), Arrays.asList(clientResponse4.cookies().get(key).stream().toList().get(0).getValue()));
                    }

                    System.out.println("statusCode:" + clientResponse4.statusCode());
                    return clientResponse4.bodyToMono(String.class);
                }).block();


        System.out.println("Tokens:" + resp);

        return resp;

    }

    public static Map<String, List<String>> splitQuery(URL url) {
        if (url.getQuery() == null || url.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(url.getQuery().split("&"))
                .map(s -> splitQueryParameter(s))
                .collect(Collectors.groupingBy(o -> o.getKey(), LinkedHashMap::new, mapping(Map.Entry::getValue, toList())));
    }

    public static AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new AbstractMap.SimpleImmutableEntry<>(
                URLDecoder.decode(key, StandardCharsets.UTF_8),
                URLDecoder.decode(value, StandardCharsets.UTF_8)
        );
    }

    private void acceptedCodecs(ClientCodecConfigurer clientCodecConfigurer) {
        //clientCodecConfigurer.defaultCodecs().maxInMemorySize();
        clientCodecConfigurer.customCodecs().encoder(new Jackson2JsonEncoder(new ObjectMapper(), TEXT_HTML));
        clientCodecConfigurer.customCodecs().decoder(new Jackson2JsonDecoder(new ObjectMapper(), TEXT_HTML));
    }

    /**
     * Mimics user login via rest request and returns an access token
     * This is useful for cases where user logs in/registers with Google/Facebook etc. account hence already have
     * a valid mail address. So that an access token can be issued immediately.
     * @param email
     * @param pass
     * @return
     */
    public TokenDto getAccessToken(String email, String pass){
        Map<String, String> bodyValues = new HashMap<>();
        bodyValues.put("emailUsername",email);
        bodyValues.put("pass",pass);

        WebClient client = WebClient.builder()
                .baseUrl(authServerEmailPasswordLoginUrl)
                .build();

        ClientResponse resp = client.post()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(bodyValues))
                .exchange()
                .block(Duration.ofSeconds(10));
        System.out.println("accessTokenUtilCallStatus:" + resp.statusCode());
        System.out.println("respBody:" + resp.bodyToMono(String.class));
        TokenDto tokenDto = resp.bodyToMono(TokenDto.class).block();
        System.out.println("token:" + tokenDto.getAccess_token());
        return tokenDto;

    }
}
