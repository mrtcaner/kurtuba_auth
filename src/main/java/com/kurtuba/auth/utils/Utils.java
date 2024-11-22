package com.kurtuba.auth.utils;

import java.util.Random;

public class Utils {

    public static String generateValidationCode(){
        int code = new Random().nextInt(999999);
        String numStr = String.valueOf(code);
        while(numStr.length() < 6){
            numStr = "0" + numStr;
        }
        return numStr;
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
}


