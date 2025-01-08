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
        htmlFileContent = htmlFileContent.replace("${prologue}", verificationMailDto.getPrologue());
        htmlFileContent = htmlFileContent.replace("${verificationLink}", verificationMailDto.getVerificationLink());
        htmlFileContent = htmlFileContent.replace("${verifyEmail}", verificationMailDto.getVerifyEmailBtnLabel());
        htmlFileContent = htmlFileContent.replace("${verificationCode}", verificationMailDto.getVerificationCode());
        htmlFileContent = htmlFileContent.replace("${displayCode}", verificationMailDto.getDisplayCode());
        htmlFileContent = htmlFileContent.replace("${displayLink}", verificationMailDto.getDisplayLink());
        htmlFileContent = htmlFileContent.replace("${epilogue}", verificationMailDto.getEpilogue());
        htmlFileContent = htmlFileContent.replace("${closing}", verificationMailDto.getClosing());
        htmlFileContent = htmlFileContent.replace("${closingSubject}", verificationMailDto.getClosingSubject());
        htmlFileContent = htmlFileContent.replace("${getInTouch}", verificationMailDto.getClosingSubject());
        return htmlFileContent;
    }

}
