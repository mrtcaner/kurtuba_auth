package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.EmailValidationMailDto;
import com.kurtuba.auth.data.enums.EmailJobStateType;
import com.kurtuba.auth.data.enums.MailType;
import com.kurtuba.auth.data.enums.MetaChangeType;
import com.kurtuba.auth.data.model.EmailJob;
import com.kurtuba.auth.data.repository.EmailJobRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.EmailUtils;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailJobService {

    @Value("${auth.server.protocol}")
    private String authServerProtocol;

    @Value("${auth.server.ip}")
    private String authServerIp;

    @Value("${auth.server.port}")
    private String authServerPort;

    @Value("${kurtuba.email.max-try-count}")
    private String emailSendMaxTryCount;

    final
    EmailJobRepository emailJobRepository;

    public EmailJobService(EmailJobRepository emailJobRepository) {
        this.emailJobRepository = emailJobRepository;
    }

    @Transactional
    public void saveEmailJob(EmailJob emailJob) {
        emailJobRepository.save(emailJob);
    }

    public List<EmailJob> findByStateAndSendAfterDateBefore(EmailJobStateType jobState, LocalDateTime before) {
        return emailJobRepository.findByStateAndSendAfterDateBefore(jobState, before);
    }

    @Transactional
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
            String htmlFileContent = EmailUtils.setRegistrationEmailValidationMessageBody(validationMailDto);

            emailJobRepository.save(EmailJob.builder()
                    .createdDate(LocalDateTime.now())
                    .mailType(MailType.ACCOUNT_ACTIVATION)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(EmailJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Email Validation Code")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
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
            String htmlFileContent = EmailUtils.setRegistrationEmailValidationMessageBody(validationMailDto);

            emailJobRepository.save(EmailJob.builder()
                    .createdDate(LocalDateTime.now())
                    .mailType(MailType.ACCOUNT_ACTIVATION)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(EmailJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Email Validation")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());

        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendPasswordResetCodeMail(@NotEmpty String recipient, @NotEmpty String resetCode) {

        try {
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailPasswordReset.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${resetCode}", resetCode);
            htmlFileContent = htmlFileContent.replace("${displayCode}", "block");
            htmlFileContent = htmlFileContent.replace("${displayLink}", "none");

            emailJobRepository.save(EmailJob.builder()
                    .createdDate(LocalDateTime.now())
                    .mailType(MailType.PASSWORD_RESET)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(EmailJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Password Reset Code")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }

    }

    @Transactional
    public void sendPasswordResetLinkMail(String recipient, String resetCode) {
        String resetLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/user/password/reset/password-reset/" + resetCode;
        try {
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailPasswordReset.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${resetLink}", resetLink);
            htmlFileContent = htmlFileContent.replace("${displayLink}", "block");
            htmlFileContent = htmlFileContent.replace("${displayCode}", "none");

            emailJobRepository.save(EmailJob.builder()
                    .createdDate(LocalDateTime.now())
                    .mailType(MailType.PASSWORD_RESET)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(EmailJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Password Reset")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

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
            String htmlFileContent = EmailUtils.setRegistrationEmailValidationMessageBody(validationMailDto);

            emailJobRepository.save(EmailJob.builder()
                    .createdDate(LocalDateTime.now())
                    .mailType(MailType.EMAIL_CHANGE)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(EmailJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Email Validation Code")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
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
            String htmlFileContent = EmailUtils.setRegistrationEmailValidationMessageBody(validationMailDto);

            emailJobRepository.save(EmailJob.builder()
                    .createdDate(LocalDateTime.now())
                    .mailType(MailType.EMAIL_CHANGE)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(EmailJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Email Validation")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserMetaChangeNotificationMail(String recipient, MetaChangeType metaChangeType) {
        String metaName = metaChangeType == MetaChangeType.PASSWORD_CHANGE || metaChangeType == MetaChangeType.PASSWORD_RESET ? "password" :
                metaChangeType.name().toLowerCase();
        try {
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailUserMetaChangeNotification.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replaceAll("metaName", metaName);

            emailJobRepository.save(EmailJob.builder()
                    .createdDate(LocalDateTime.now())
                    .mailType(MailType.NOTIFICATION)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(EmailJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Account Modification")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

}
