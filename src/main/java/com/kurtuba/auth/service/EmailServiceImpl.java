package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.EmailDetails;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Date;


@Service
@Validated
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
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

}