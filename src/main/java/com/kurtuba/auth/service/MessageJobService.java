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
    LocalizationService localizationService;

    public MessageJobService(MessageJobRepository messageJobRepository, LocalizationService localizationService) {
        this.messageJobRepository = messageJobRepository;
        this.localizationService = localizationService;
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
                .title(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.title")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .greet(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.greet")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .prologue(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.code.prologue")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .verificationCode(verificationCode)
                .verificationLink("")
                .verifyEmailBtnLabel("")
                .displayCode("block")
                .displayLink("none")
                .epilogue(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.code.epilogue")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .closing(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.closing")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .closingSubject(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.closing.subject")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .getInTouch(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.get-in-touch")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
                    .subject(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.subject")
                            .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
                "auth/registration/activation/link/" + verificationCode;

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.title")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .greet(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.greet")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .prologue(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.link.prologue")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .verificationLink(verificationLink)
                .verificationCode("")
                .verifyEmailBtnLabel(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.link.button.label")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .displayCode("none")
                .displayLink("block")
                .epilogue("")
                .closing(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.closing")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .closingSubject(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.closing.subject")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .getInTouch(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.content.get-in-touch")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
                    .subject(localizationService.findByLanguageCodeAndKey(lang, "mail.account.activation.subject")
                            .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
            htmlFileContent = htmlFileContent.replace("${prologue}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.code.prologue")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${epilogue}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.epilogue")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${closing}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.closing")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${closingSubject}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.closing.subject")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${getInTouch}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.get-in-touch")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.subject")
                            .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
            htmlFileContent = htmlFileContent.replace("${prologue}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.link.prologue")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${epilogue}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.epilogue")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${closing}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.closing")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${closingSubject}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.closing.subject")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${getInTouch}", localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.content.get-in-touch")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationService.findByLanguageCodeAndKey(lang, "mail.password.reset.subject")
                            .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
                .title(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.title")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .greet(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.greet")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .prologue(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.code.prologue")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .verificationCode(verificationCode)
                .verificationLink("")
                .verifyEmailBtnLabel("")
                .displayCode("block")
                .displayLink("none")
                .epilogue("")
                .closing(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.closing")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .closingSubject(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.closing.subject")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .getInTouch(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.get-in-touch")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
                    .subject(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.subject")
                            .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
                .title(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.title")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .greet(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.greet")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .prologue(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.link.prologue")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .verificationLink(verificationLink)
                .verifyEmailBtnLabel(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.link.button.label")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .verificationCode("")
                .displayCode("none")
                .displayLink("block")
                .epilogue("")
                .closing(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.closing")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .closingSubject(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.closing.subject")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
                .getInTouch(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.content.get-in-touch")
                        .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
                    .subject(localizationService.findByLanguageCodeAndKey(lang, "mail.email.verification.subject")
                            .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
            htmlFileContent = htmlFileContent.replace("${greet}", localizationService.findByLanguageCodeAndKey(lang, "mail.account.modification.content.greet")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${prologue}", localizationService.findByLanguageCodeAndKey(lang, "mail.account.modification.content.prologue")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${context}", localizationService.findByLanguageCodeAndKey(lang, "mail.account.modification.content.context")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${epilogue}", localizationService.findByLanguageCodeAndKey(lang, "mail.account.modification.content.epilogue")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${closing}", localizationService.findByLanguageCodeAndKey(lang, "mail.account.modification.content.closing")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${closingSubject}", localizationService.findByLanguageCodeAndKey(lang, "mail.account.modification.content.closing.subject")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replace("${getInTouch}", localizationService.findByLanguageCodeAndKey(lang, "mail.account.modification.content.get-in-touch")
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage());
            htmlFileContent = htmlFileContent.replaceAll("metaName", metaName);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .maxTryCount(Integer.valueOf(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationService.findByLanguageCodeAndKey(lang, "mail.account.modification.subject")
                            .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_INVALID_RESOURCE_PARAMETER)).getMessage())
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
