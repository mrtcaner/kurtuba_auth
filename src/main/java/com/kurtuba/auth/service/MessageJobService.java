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

    final
    LocalizationMessageService localizationMessageService;

    public MessageJobService(MessageJobRepository messageJobRepository, LocalizationMessageService localizationMessageService) {
        this.messageJobRepository = messageJobRepository;
        this.localizationMessageService = localizationMessageService;
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
    public void sendAccountActivationCodeMail(@NotEmpty String recipient, @NotEmpty String verificationCode, String lang) {

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.code.prologue").getMessage())
                .verificationCode(verificationCode)
                .verificationLink("")
                .verifyEmailBtnLabel("")
                .displayCode("block")
                .displayLink("none")
                .epilogue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.code.epilogue").getMessage())
                .closing(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.get-in-touch").getMessage())
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
                    .subject(localizationMessageService
                            .findByLanguageCodeAndKey(lang, "mail.account.activation.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendAccountActivationLinkMail(String recipient, String verificationCode, String lang) {

        String verificationLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/auth/registration/activation/link/" + verificationCode;

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.link.prologue").getMessage())
                .verificationLink(verificationLink)
                .verificationCode("")
                .verifyEmailBtnLabel(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.link.button.label").getMessage())
                .displayCode("none")
                .displayLink("block")
                .epilogue("")
                .closing(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.get-in-touch").getMessage())
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
                    .subject(localizationMessageService
                            .findByLanguageCodeAndKey(lang, "mail.account.activation.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());

        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendPasswordResetCodeMail(@NotEmpty String recipient, @NotEmpty String resetCode, String lang) {

        try {
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailPasswordReset.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${resetCode}", resetCode);
            htmlFileContent = htmlFileContent.replace("${displayCode}", "block");
            htmlFileContent = htmlFileContent.replace("${displayLink}", "none");
            htmlFileContent = htmlFileContent.replace("${prologue}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.code.prologue").getMessage());
            htmlFileContent = htmlFileContent.replace("${epilogue}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.epilogue").getMessage());
            htmlFileContent = htmlFileContent.replace("${closing}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.closing").getMessage());
            htmlFileContent = htmlFileContent.replace("${closingSubject}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.closing.subject").getMessage());
            htmlFileContent = htmlFileContent.replace("${getInTouch}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.get-in-touch").getMessage());

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.password.reset.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }

    }

    @Transactional
    public void sendPasswordResetLinkMail(String recipient, String resetCode, String lang) {

        String resetLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/user/password/reset/password-reset/" + resetCode;
        try {
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailPasswordReset.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${resetLink}", resetLink);
            htmlFileContent = htmlFileContent.replace("${displayLink}", "block");
            htmlFileContent = htmlFileContent.replace("${displayCode}", "none");
            htmlFileContent = htmlFileContent.replace("${prologue}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.link.prologue").getMessage());
            htmlFileContent = htmlFileContent.replace("${epilogue}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.epilogue").getMessage());
            htmlFileContent = htmlFileContent.replace("${closing}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.closing").getMessage());
            htmlFileContent = htmlFileContent.replace("${closingSubject}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.closing.subject").getMessage());
            htmlFileContent = htmlFileContent.replace("${getInTouch}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.password.reset.content.get-in-touch").getMessage());

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.password.reset.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserEmailChangeCodeMail(String recipient, String verificationCode, String lang) {

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.code.prologue").getMessage())
                .verificationCode(verificationCode)
                .verificationLink("")
                .verifyEmailBtnLabel("")
                .displayCode("block")
                .displayLink("none")
                .epilogue("")
                .closing(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.get-in-touch").getMessage())
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
                    .subject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserEmailChangeLinkMail(String recipient, String verificationCode, String lang) {

        String verificationLink = authServerProtocol + authServerIp + ":" + authServerPort +
                "/user/email/verification/link/" + verificationCode;

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.link.prologue").getMessage())
                .verificationLink(verificationLink)
                .verifyEmailBtnLabel(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.link.button.label").getMessage())
                .verificationCode("")
                .displayCode("none")
                .displayLink("block")
                .epilogue("")
                .closing(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.get-in-touch").getMessage())
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
                    .subject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserMetaChangeNotificationMail(String recipient, MetaOperationType metaOperationType, String lang) {

        String metaName = metaOperationType == MetaOperationType.PASSWORD_CHANGE || metaOperationType == MetaOperationType.PASSWORD_RESET ? "password" :
                "email";
        try {
            File htmlFile = ResourceUtils.getFile("classpath:templates/mailUserMetaChangeNotification.html");
            String htmlFileContent = new String(Files.readAllBytes(htmlFile.toPath()));
            htmlFileContent = htmlFileContent.replace("${greet}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.account.modification.content.greet").getMessage());
            htmlFileContent = htmlFileContent.replace("${prologue}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.account.modification.content.prologue").getMessage());
            htmlFileContent = htmlFileContent.replace("${context}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.account.modification.content.context").getMessage());
            htmlFileContent = htmlFileContent.replace("${epilogue}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.account.modification.content.epilogue").getMessage());
            htmlFileContent = htmlFileContent.replace("${closing}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.account.modification.content.closing").getMessage());
            htmlFileContent = htmlFileContent.replace("${closingSubject}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.account.modification.content.closing.subject").getMessage());
            htmlFileContent = htmlFileContent.replace("${getInTouch}", localizationMessageService
                    .findByLanguageCodeAndKey(lang, "mail.account.modification.content.get-in-touch").getMessage());
            htmlFileContent = htmlFileContent.replaceAll("metaName", metaName);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.modification.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserMetaChangeNotificationSMS(String recipient, MetaOperationType metaOperationType, String lang) {
        // todo implement send sms
        throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
    }

}
