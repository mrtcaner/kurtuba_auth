package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.EmailVerificationMailDto;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MessageJobStateType;
import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.model.MessageJob;
import com.kurtuba.auth.data.repository.MessageJobRepository;
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
public class MessageJobService {

    @Value("${auth.server.protocol}")
    private String authServerProtocol;

    @Value("${auth.server.ip}")
    private String authServerIp;

    @Value("${auth.server.port}")
    private String authServerPort;

    @Value("${kurtuba.job.email.send.max-try-count}")
    private String emailSendMaxTryCount;

    final
    MessageJobRepository messageJobRepository;

    public MessageJobService(MessageJobRepository messageJobRepository) {
        this.messageJobRepository = messageJobRepository;
    }

    @Transactional
    public void saveEmailJob(MessageJob messageJob) {
        messageJobRepository.save(messageJob);
    }

    public List<MessageJob> findByStateAndSendAfterDateBefore(MessageJobStateType jobState, LocalDateTime before) {
        return messageJobRepository.findByStateAndSendAfterDateBefore(jobState, before);
    }

    public List<MessageJob> findByStateAndContactTypeAndSendAfterDateBefore(MessageJobStateType jobState,
                                                                            ContactType contactType, LocalDateTime before) {
        return messageJobRepository.findByStateAndContactTypeAndSendAfterDateBefore(jobState, contactType, before);
    }

    @Transactional
    public void sendAccountActivationCodeMail(@NotEmpty String recipient, @NotEmpty String verificationCode) {
        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title("THANKS FOR SIGNING UP!")
                .greet("Hi")
                .msg1("You're almost ready to get started. Here is your activation code")
                .verificationCode(verificationCode)
                .verificationLink("")
                .displayCode("block")
                .displayLink("none")
                .msg2("You can login to Kurtuba with your existing credentials to enter the code")
                .build();
        try {
            String htmlFileContent = EmailUtils.setEmailVerificationMessageBody(verificationMailDto);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Account Activation Code")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendAccountActivationLinkMail(String recipient, String verificationCode) {
        String verificationLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "auth/registration/activation/link/" + verificationCode;

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title("THANKS FOR SIGNING UP!")
                .greet("Hi")
                .msg1("You're almost ready to get started. Click below to verify your mail address")
                .verificationLink(verificationLink)
                .verificationCode("")
                .displayCode("none")
                .displayLink("block")
                .msg2("")
                .build();

        try {
            String htmlFileContent = EmailUtils.setEmailVerificationMessageBody(verificationMailDto);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Account Activation")
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

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
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

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
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

    @Transactional
    public void sendUserEmailChangeCodeMail(String recipient, String verificationCode) {

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title("Verify Your E-mail Address!")
                .greet("Hi")
                .msg1("Here is your verification code")
                .verificationCode(verificationCode)
                .verificationLink("")
                .displayCode("block")
                .displayLink("none")
                .msg2("")
                .build();

        try {
            String htmlFileContent = EmailUtils.setEmailVerificationMessageBody(verificationMailDto);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Email Verification Code")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserEmailChangeLinkMail(String recipient, String verificationCode) {

        String verificationLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/auth/email/verification/link/" + verificationCode;

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title("Verify Your E-mail Address!")
                .greet("Hi")
                .msg1("Click below to verify your email address")
                .verificationLink(verificationLink)
                .verificationCode("")
                .displayCode("none")
                .displayLink("block")
                .msg2("")
                .build();

        try {
            String htmlFileContent = EmailUtils.setEmailVerificationMessageBody(verificationMailDto);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject("Kurtuba Email Verification")
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserMetaChangeNotificationMail(String recipient, MetaOperationType metaOperationType) {
        String metaName = metaOperationType == MetaOperationType.PASSWORD_CHANGE || metaOperationType == MetaOperationType.PASSWORD_RESET ? "password" :
                metaOperationType.name().toLowerCase();
        try {
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailUserMetaChangeNotification.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replaceAll("metaName", metaName);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
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

    @Transactional
    public void sendUserMetaChangeNotificationSMS(String recipient, MetaOperationType metaOperationType) {
        // todo implement send sms
        throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
    }

}
