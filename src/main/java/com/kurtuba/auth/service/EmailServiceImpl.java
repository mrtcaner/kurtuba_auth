package com.kurtuba.auth.service;// Importing required classes

import com.kurtuba.auth.data.dto.EmailValidationMailDto;
import com.kurtuba.auth.data.enums.MetaChangeType;
import com.kurtuba.auth.data.model.EmailDetails;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.EmailUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;


@Service
@Validated
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${auth.server.protocol}")
    private String authServerProtocol;

    @Value("${auth.server.ip}")
    private String authServerIp;

    @Value("${auth.server.port}")
    private String authServerPort;

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendRegistrationValidationCodeMail(@NotEmpty String recipient, @NotEmpty String validationCode) {
        EmailValidationMailDto validationMailDto = EmailValidationMailDto.builder()
                .title("THANKS FOR SIGNING UP!")
                .greet("Hi")
                .msg1("You're almost ready to get started. Here is your validation code")
                .validationCode(validationCode)
                .displayCode("block")
                .displayLink("none")
                .msg2("Login to Kurtuba with your existing credentials to enter the code")
                .build();
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Email Validation Code");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(EmailUtils.setRegistrationEmailValidationMessageBody(validationMailDto), "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(),e.getMessage());
        }

    }

    @Override
    public void sendRegistrationValidationLinkMail(String recipient, String validationCode) {
        String validationLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/auth/email/validation/link/" + validationCode;

        EmailValidationMailDto validationMailDto = EmailValidationMailDto.builder()
                .title("THANKS FOR SIGNING UP!")
                .greet("Hi")
                .msg1("You're almost ready to get started. Click below to verify your mail address")
                .validationLink(validationLink)
                .displayCode("none")
                .displayLink("block")
                .msg2("")
                .build();

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Email Validation");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(EmailUtils.setRegistrationEmailValidationMessageBody(validationMailDto), "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(),e.getMessage());
        }
    }

    public void sendPasswordResetCodeMail(@NotEmpty String recipient, @NotEmpty String resetCode) {

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Password Reset Code");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailPasswordReset.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${resetCode}", resetCode);
            htmlFileContent = htmlFileContent.replace("${displayCode}", "block");
            htmlFileContent = htmlFileContent.replace("${displayLink}", "none");
            messageBodyPart.setContent(htmlFileContent, "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(),e.getMessage());
        }

    }

    @Override
    public void sendPasswordResetLinkMail(String recipient, String resetCode) {
        String resetLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/user/password/reset/password-reset/" + resetCode;
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Password Reset Code");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailPasswordReset.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${resetLink}", resetLink);
            htmlFileContent = htmlFileContent.replace("${displayLink}", "block");
            htmlFileContent = htmlFileContent.replace("${displayCode}", "none");
            messageBodyPart.setContent(htmlFileContent, "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(),e.getMessage());
        }
    }

    @Override
    public void sendUserEmailChangeCodeMail(String recipient, String validationCode) {

        EmailValidationMailDto validationMailDto = EmailValidationMailDto.builder()
                .title("Validate Your E-mail Address!")
                .greet("Hi")
                .msg1("Here is your validation code")
                .validationCode(validationCode)
                .displayCode("block")
                .displayLink("none")
                .msg2("")
                .build();

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Email Validation Code");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(EmailUtils.setRegistrationEmailValidationMessageBody(validationMailDto), "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(),e.getMessage());
        }
    }

    @Override
    public void sendUserEmailChangeLinkMail(String recipient, String validationCode) {

        String validationLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/auth/email/validation/link/" + validationCode;

        EmailValidationMailDto validationMailDto = EmailValidationMailDto.builder()
                .title("Validate Your E-mail Address!")
                .greet("Hi")
                .msg1("Click below to validate your email address")
                .validationLink(validationLink)
                .displayCode("none")
                .displayLink("block")
                .msg2("")
                .build();

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Email Validation");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(EmailUtils.setRegistrationEmailValidationMessageBody(validationMailDto), "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Override
    public void sendUserMetaChangeNotificationMail(String recipient, MetaChangeType metaChangeType) {
        String metaName = metaChangeType == MetaChangeType.PASSWORD_CHANGE || metaChangeType == MetaChangeType.PASSWORD_RESET ? "password" :
                metaChangeType.name().toLowerCase();
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Email Validation");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailUserMetaChangeNotification.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replaceAll("metaName", metaName);
            messageBodyPart.setContent(htmlFileContent, "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(),e.getMessage());
        }
    }

    @Override
    public void sendMultipartMail(EmailDetails details) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom(details.getSender());
            message.setRecipients(MimeMessage.RecipientType.TO, details.getRecipient());
            message.setSubject(details.getSubject());
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(details.getMsgBody(), "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    public String sendSimpleMail(EmailDetails details) {

        // Try block to check for exceptions
        try {

            // Creating a simple mail message
            SimpleMailMessage mailMessage
                    = new SimpleMailMessage();

            // Setting up necessary details
            mailMessage.setFrom("test@test.com");
            mailMessage.setTo(details.getRecipient());
            mailMessage.setText(details.getMsgBody());
            mailMessage.setSubject(details.getSubject());

            // Sending the mail
            javaMailSender.send(mailMessage);
            return "Mail Sent Successfully...";
        }

        // Catch block to handle the exceptions
        catch (Exception e) {
            e.printStackTrace();
            return "Error while Sending Mail";
        }
    }

    public String sendMailWithAttachment(EmailDetails details) {
        // Creating a mime message
        MimeMessage mimeMessage
                = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;

        try {

            // Setting multipart as true for attachments to
            // be send
            mimeMessageHelper
                    = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom("test@test.com");
            mimeMessageHelper.setTo(details.getRecipient());
            mimeMessageHelper.setText(details.getMsgBody());
            mimeMessageHelper.setSubject(
                    details.getSubject());

            // Adding the attachment
            FileSystemResource file
                    = new FileSystemResource(
                    new File(details.getAttachment()));

            mimeMessageHelper.addAttachment(
                    file.getFilename(), file);

            // Sending the mail
            javaMailSender.send(mimeMessage);
            return "Mail sent Successfully";
        }

        // Catch block to handle MessagingException
        catch (MessagingException e) {

            // Display message when exception occurred
            return "Error while sending mail!!!";
        }
    }
}