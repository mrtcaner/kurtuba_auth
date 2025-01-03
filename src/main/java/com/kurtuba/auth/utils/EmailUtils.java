package com.kurtuba.auth.utils;

import com.kurtuba.auth.data.dto.EmailVerificationMailDto;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class EmailUtils {


    public static String setEmailVerificationMessageBody(EmailVerificationMailDto verificationMailDto) throws IOException {
        File htmlFile = ResourceUtils.getFile("classpath:templates/mailEmailVerification.html");
        String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
        htmlFileContent = htmlFileContent.replace("${title}", verificationMailDto.getTitle());
        htmlFileContent = htmlFileContent.replace("${greet}", verificationMailDto.getGreet());
        htmlFileContent = htmlFileContent.replace("${msg1}", verificationMailDto.getMsg1());
        htmlFileContent = htmlFileContent.replace("${verificationLink}", verificationMailDto.getVerificationLink());
        htmlFileContent = htmlFileContent.replace("${verificationCode}", verificationMailDto.getVerificationCode());
        htmlFileContent = htmlFileContent.replace("${displayCode}", verificationMailDto.getDisplayCode());
        htmlFileContent = htmlFileContent.replace("${displayLink}", verificationMailDto.getDisplayLink());
        htmlFileContent = htmlFileContent.replace("${msg2}", verificationMailDto.getMsg2());
        return htmlFileContent;
    }

}
