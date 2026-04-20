package com.kurtuba.auth.utils;

import com.kurtuba.auth.data.dto.EmailVerificationMailDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EmailUtils {


    public static String setEmailVerificationMessageBody(EmailVerificationMailDto verificationMailDto) throws IOException {
        String htmlFileContent = loadTemplate("templates/mailEmailVerification.html");
        htmlFileContent = htmlFileContent.replace("${title}", verificationMailDto.getTitle());
        htmlFileContent = htmlFileContent.replace("${greet}", verificationMailDto.getGreet());
        htmlFileContent = htmlFileContent.replace("${prologue}", verificationMailDto.getPrologue());
        htmlFileContent = htmlFileContent.replace("${verificationLink}", verificationMailDto.getVerificationLink());
        htmlFileContent = htmlFileContent.replace("${verifyEmail}", verificationMailDto.getVerifyEmailBtnLabel());
        htmlFileContent = htmlFileContent.replace("${verificationCode}", verificationMailDto.getVerificationCode());
        htmlFileContent = htmlFileContent.replace("${displayCode}", verificationMailDto.getDisplayCode());
        htmlFileContent = htmlFileContent.replace("${displayLink}", verificationMailDto.getDisplayLink());
        htmlFileContent = htmlFileContent.replace("${epilogue}", verificationMailDto.getEpilogue());
        htmlFileContent = htmlFileContent.replace("${closing}", verificationMailDto.getClosing());
        htmlFileContent = htmlFileContent.replace("${closingSubject}", verificationMailDto.getClosingSubject());
        htmlFileContent = htmlFileContent.replace("${getInTouch}", verificationMailDto.getGetInTouch());
        htmlFileContent = htmlFileContent.replace("${supportEmail}", verificationMailDto.getSupportEmail());
        return htmlFileContent;
    }

    public static String loadTemplate(String templatePath) throws IOException {
        ClassPathResource templateResource = new ClassPathResource(templatePath);
        try (var inputStream = templateResource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

}
