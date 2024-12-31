package com.kurtuba.auth.utils;

import com.kurtuba.auth.data.dto.EmailValidationMailDto;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class EmailUtils {


    public static String setRegistrationEmailValidationMessageBody(EmailValidationMailDto validationMailDto) throws IOException {
        File htmlFile = ResourceUtils.getFile("classpath:templates/mailEmailValidation.html");
        String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
        htmlFileContent = htmlFileContent.replace("${title}", validationMailDto.getTitle());
        htmlFileContent = htmlFileContent.replace("${greet}", validationMailDto.getGreet());
        htmlFileContent = htmlFileContent.replace("${msg1}", validationMailDto.getMsg1());
        htmlFileContent = htmlFileContent.replace("${validationLink}", validationMailDto.getValidationLink());
        htmlFileContent = htmlFileContent.replace("${displayCode}", validationMailDto.getDisplayCode());
        htmlFileContent = htmlFileContent.replace("${displayLink}", validationMailDto.getDisplayLink());
        htmlFileContent = htmlFileContent.replace("${msg2}", validationMailDto.getMsg2());
        return htmlFileContent;
    }

}
