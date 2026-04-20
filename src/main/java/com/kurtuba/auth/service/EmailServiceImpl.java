package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.EmailDetails;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Date;
import java.util.regex.Pattern;


@Service
@Profile("!local")
@Validated
public class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern HTML_BREAK_PATTERN = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HTML_BLOCK_END_PATTERN =
            Pattern.compile("(?i)</(p|div|h1|h2|h3|h4|h5|h6|li|tr|table|section)>"); 
    private static final Pattern MULTI_NEWLINE_PATTERN = Pattern.compile("\\n{3,}");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("[\\t\\x0B\\f\\r ]+");

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

            MimeBodyPart textBodyPart = new MimeBodyPart();
            textBodyPart.setText(toPlainText(details.getMsgBody()), "UTF-8");

            MimeBodyPart htmlBodyPart = new MimeBodyPart();
            htmlBodyPart.setContent(details.getMsgBody(), "text/html; charset=UTF-8");

            MimeMultipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(textBodyPart);
            multipart.addBodyPart(htmlBodyPart);
            message.setContent(multipart);
            javaMailSender.send(message);
        } catch (Exception e) {
            String causeMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            LOGGER.error("Failed to send multipart mail. recipient={}, subject={}, cause={}",
                    details.getRecipient(), details.getSubject(), causeMessage, e);
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(),
                    ErrorEnum.MAIL_UNABLE_TO_SEND.getMessage() + ": " + causeMessage, e);
        }
    }

    private String toPlainText(String html) {
        String text = HTML_BREAK_PATTERN.matcher(html).replaceAll("\n");
        text = HTML_BLOCK_END_PATTERN.matcher(text).replaceAll("\n");
        text = text.replace("&nbsp;", " ");
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&#39;", "'");
        text = text.replace("&quot;", "\"");
        text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
        text = MULTI_SPACE_PATTERN.matcher(text).replaceAll(" ");
        text = text.replace(" \n", "\n");
        text = text.replace("\n ", "\n");
        text = MULTI_NEWLINE_PATTERN.matcher(text.trim()).replaceAll("\n\n");
        return text;
    }

}
