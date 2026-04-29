package com.kurtuba.auth.utils;

import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;

import java.time.Instant;
import java.util.Random;
import java.util.stream.Stream;

public class Utils {

    public static final String EMAIL_REGEX = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[A-Za-z0-9][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

    public static final String USERNAME_REGEX = "^(?=.{2,32}$)(?![_.])(?!.*[_.]{2})[a-z0-9._]+(?<![_.])$";

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\.@$!%*#?&])[A-Za-z\\d\\.@$!%*#?&]{6,}$";

    public static final RegisteredClient DEFAULT_CLIENT = RegisteredClient.builder()
                                                                          .clientId("default-client")
                                                                          .clientName("default-client")
                                                                          .clientType(RegisteredClientType.DEFAULT)
                                                                          .scopeEnabled(false)
                                                                          .accessTokenTtlMinutes(5)
                                                                          .refreshTokenEnabled(true)
                                                                          .refreshTokenTtlMinutes(129600)
                                                                          .sendTokenInCookie(false)
                                                                          .cookieHttpOnly(false)
                                                                          .cookieSecure(false)
                                                                          .createdDate(Instant.now())
                                                                          .build();

    public static String generateVerificationCode(){
        int code = new Random().nextInt(999999);
        StringBuilder numStr = new StringBuilder(String.valueOf(code));
        Stream.generate(() -> numStr).limit(6 - numStr.length( )).forEach(stringBuilder -> stringBuilder.insert(0, "0"));
        return numStr.toString();
    }

    public static String generateRandomAlphanumericString(int targetStringLength){
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i >= 48 && i <= 57) || (i >= 97 && i <= 122))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static String generateRandomNumericString(int targetStringLength){
        int leftLimit = 48; // numeral '0'
        int rightLimit = 57; // numeral '9'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
                     .filter(i -> (i >= 48 && i <= 57))
                     .limit(targetStringLength)
                     .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                     .toString();
    }

    public static String generateRandomString(int targetStringLength){
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
                     .filter(i -> (i >= 97 && i <= 122))
                     .limit(targetStringLength)
                     .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                     .toString();
    }
}

