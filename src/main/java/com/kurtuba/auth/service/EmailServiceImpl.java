package com.kurtuba.auth.service;// Importing required classes

import com.kurtuba.auth.data.model.EmailDetails;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
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

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${auth.server.protocol}")
    private String authServerProtocol;

    @Value("${auth.server.ip}")
    private String authServerIp;

    @Value("${auth.server.port}")
    private String authServerPort;

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendValidationCodeMail(@NotEmpty String recipient, @NotEmpty String validationCode) {

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Validation Code");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailEmailValidationCode.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${validationCode}", validationCode);
            messageBodyPart.setContent(htmlFileContent, "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND);
        }

    }

    @Override
    public void sendValidationLinkMail(String recipient, String validationCode) {

        String validationLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/auth/register/email/validation/link/" + validationCode;
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Validation Code");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailEmailValidationLink.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${validationLink}", validationLink);
            messageBodyPart.setContent(htmlFileContent, "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND);
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
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailPasswordResetCode.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${resetCode}", resetCode);
            messageBodyPart.setContent(htmlFileContent, "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND);
        }

    }

    @Override
    public void sendPasswordResetLinkMail(String recipient, String resetCode) {
        String resetLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/user/password-reset/" + resetCode;
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom("sender-test@example.com");
            message.setRecipients(MimeMessage.RecipientType.TO, recipient);
            message.setSubject("Kurtuba Password Reset Code");
            message.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailPasswordResetLink.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${resetLink}", resetLink);
            messageBodyPart.setContent(htmlFileContent, "text/html");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND);
        }
    }

    // Method 1
    // To send a simple email
    public String sendSimpleMail(EmailDetails details) {

        // Try block to check for exceptions
        try {

            // Creating a simple mail message
            SimpleMailMessage mailMessage
                    = new SimpleMailMessage();

            // Setting up necessary details
            mailMessage.setFrom(sender);
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

    // Method 2
    // To send an email with attachment
    public String
    sendMailWithAttachment(EmailDetails details) {
        // Creating a mime message
        MimeMessage mimeMessage
                = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;

        try {

            // Setting multipart as true for attachments to
            // be send
            mimeMessageHelper
                    = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(sender);
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