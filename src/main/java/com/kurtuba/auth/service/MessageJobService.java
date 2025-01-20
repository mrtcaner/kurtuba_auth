package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.EmailVerificationMailDto;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MessageJobStateType;
import com.kurtuba.auth.data.enums.MessageServiceProviderType;
import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.model.MessageJob;
import com.kurtuba.auth.data.repository.MessageJobRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.EmailUtils;
import com.kurtuba.auth.utils.annotation.MobileNumber;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class MessageJobService {

    @Value("${kurtuba.server.url}")
    private String kurtubaServerUrl;

    @Value("${kurtuba.job.email.send.max-try-count}")
    private String emailSendMaxTryCount;

    @Value("${kurtuba.job.sms.send.max-try-count}")
    private String smsSendMaxTryCount;

    final
    MessageJobRepository messageJobRepository;

    final
    LocalizationMessageService localizationMessageService;

    public MessageJobService(MessageJobRepository messageJobRepository,
                             LocalizationMessageService localizationMessageService) {
        this.messageJobRepository = messageJobRepository;
        this.localizationMessageService = localizationMessageService;
    }

    @Transactional
    public void saveMessageJob(MessageJob messageJob) {
        messageJobRepository.save(messageJob);
    }

    public List<MessageJob> findByStateAndSendAfterDateBefore(MessageJobStateType jobState, LocalDateTime before) {
        return messageJobRepository.findByStateAndSendAfterDateBefore(jobState, before);
    }

    public List<MessageJob> findByStateAndContactTypeAndSendAfterDateBefore(MessageJobStateType jobState,
                                                                            ContactType contactType,
                                                                            LocalDateTime before) {
        return messageJobRepository.findByStateAndContactTypeAndSendAfterDateBefore(jobState, contactType, before);
    }

    public List<MessageJob> findByStateAndContactTypeAndMessageServiceProviderTypeAndSendAfterDateBefore(MessageJobStateType jobState,
                                                                                                         ContactType contactType, MessageServiceProviderType serviceProvider, LocalDateTime before) {
        return messageJobRepository.findByStateAndContactTypeAndMessageServiceProviderTypeAndSendAfterDateBefore(jobState, contactType, serviceProvider, before);
    }

    @Transactional
    public void sendAccountActivationCodeMail(@NotBlank String recipient, @NotBlank String verificationCode,
                                              @NotBlank String lang, String userMetaChangeId) {

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content" +
                        ".title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content" +
                        ".greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content" +
                        ".code.prologue").getMessage())
                .verificationCode(verificationCode)
                .verificationLink("")
                .verifyEmailBtnLabel("")
                .displayCode("block")
                .displayLink("none")
                .epilogue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content" +
                        ".code.epilogue").getMessage())
                .closing(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content" +
                        ".closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation" +
                        ".content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation" +
                        ".content.get-in-touch").getMessage())
                .build();
        createAccountActivationMessageJob(recipient, lang, verificationMailDto, userMetaChangeId);
    }

    private void createAccountActivationMessageJob(@NotBlank String recipient, @NotBlank String lang,
                                                   EmailVerificationMailDto verificationMailDto,
                                                   String userMetaChangeId) {
        try {
            String htmlFileContent = EmailUtils.setEmailVerificationMessageBody(verificationMailDto);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService
                            .findByLanguageCodeAndKey(lang, "mail.account.activation.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendVerificationCodeSMS(@NotBlank String mobileNumber, @NotBlank String code,
                                        @NotBlank String languageCode, String userMetaChangeId) {
        messageJobRepository.save(MessageJob.builder()
                .createdDate(LocalDateTime.now())
                .contactType(ContactType.MOBILE)
                //.serviceProvider(MessageServiceProviderType.KURTUBA) // todo: let it throw exception until proper
                // implementation of regular sms send
                .maxTryCount(Integer.parseInt(smsSendMaxTryCount))
                .sendAfterDate(LocalDateTime.now())
                .state(MessageJobStateType.PENDING)
                .tryCount(0)
                .recipient(mobileNumber)
                //user languageCode here and translate
                .message("sms.account.activation.message" + code)
                .sender("sms.account.activation.sender")
                .userMetaChangeId(userMetaChangeId)
                .build());
    }

    @Transactional
    public void sendVerificationCodeSMSViaTwilio(@MobileNumber String mobileNumber, String userMetaChangeId) {
        messageJobRepository.save(MessageJob.builder()
                .createdDate(LocalDateTime.now())
                .contactType(ContactType.MOBILE)
                .serviceProvider(MessageServiceProviderType.TWILIO_VERIFY)
                .maxTryCount(Integer.parseInt(smsSendMaxTryCount))
                .sendAfterDate(LocalDateTime.now())
                .state(MessageJobStateType.PENDING)
                .tryCount(0)
                .recipient(mobileNumber)
                .message(MessageServiceProviderType.TWILIO_VERIFY.name())
                .sender(MessageServiceProviderType.TWILIO_VERIFY.name())
                .userMetaChangeId(userMetaChangeId)
                .build());
    }

    @Transactional
    public void sendAccountActivationLinkMail(String recipient, String verificationCode, String lang,
                                              String userMetaChangeId) {

        String verificationLink = kurtubaServerUrl + "/registration/activation/link/" + verificationCode;

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content" +
                        ".title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content" +
                        ".greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content" +
                        ".link.prologue").getMessage())
                .verificationLink(verificationLink)
                .verificationCode("")
                .verifyEmailBtnLabel(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account" +
                        ".activation.content.link.button.label").getMessage())
                .displayCode("none")
                .displayLink("block")
                .epilogue("")
                .closing(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation.content" +
                        ".closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation" +
                        ".content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.activation" +
                        ".content.get-in-touch").getMessage())
                .build();

        createAccountActivationMessageJob(recipient, lang, verificationMailDto, userMetaChangeId);
    }

    @Transactional
    public void sendPasswordResetCodeMail(@NotBlank String recipient, @NotBlank String resetCode, String lang,
                                          String userMetaChangeId) {

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
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.password.reset.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }

    }

    @Transactional
    public void sendPasswordResetLinkMail(String recipient, String resetCode, String lang, String userMetaChangeId) {

        String resetLink = kurtubaServerUrl + "/user/password/reset/password-reset/" + resetCode;
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
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.password.reset.subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserEmailChangeCodeMail(String recipient, String verificationCode, String lang,
                                            String userMetaChangeId) {

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content" +
                        ".title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content" +
                        ".greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content" +
                        ".code.prologue").getMessage())
                .verificationCode(verificationCode)
                .verificationLink("")
                .verifyEmailBtnLabel("")
                .displayCode("block")
                .displayLink("none")
                .epilogue("")
                .closing(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content" +
                        ".closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification" +
                        ".content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification" +
                        ".content.get-in-touch").getMessage())
                .build();

        createEmailChangeMessageJob(recipient, lang, verificationMailDto, userMetaChangeId);
    }

    private void createEmailChangeMessageJob(String recipient, String lang,
                                             EmailVerificationMailDto verificationMailDto, String userMetaChangeId) {
        try {
            String htmlFileContent = EmailUtils.setEmailVerificationMessageBody(verificationMailDto);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(LocalDateTime.now())
                    .contactType(ContactType.EMAIL)
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification" +
                            ".subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserEmailChangeLinkMail(String recipient, String verificationCode, String lang,
                                            String userMetaChangeId) {

        String verificationLink = kurtubaServerUrl + "/user/email/verification/link/" + verificationCode;

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content" +
                        ".title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content" +
                        ".greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content" +
                        ".link.prologue").getMessage())
                .verificationLink(verificationLink)
                .verifyEmailBtnLabel(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email" +
                        ".verification.content.link.button.label").getMessage())
                .verificationCode("")
                .displayCode("none")
                .displayLink("block")
                .epilogue("")
                .closing(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification.content" +
                        ".closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification" +
                        ".content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.email.verification" +
                        ".content.get-in-touch").getMessage())
                .build();

        createEmailChangeMessageJob(recipient, lang, verificationMailDto, userMetaChangeId);
    }

    @Transactional
    public void sendUserMetaChangeNotificationMail(String recipient, MetaOperationType metaOperationType, String lang,
                                                   String userMetaChangeId) {

        String metaName =
                metaOperationType == MetaOperationType.PASSWORD_CHANGE ||
                        metaOperationType == MetaOperationType.PASSWORD_RESET ? "password"
                        : metaOperationType == MetaOperationType.MOBILE_CHANGE ? "mobile number"
                        : "email address";
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
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(LocalDateTime.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndKey(lang, "mail.account.modification" +
                            ".subject").getMessage())
                    .message(htmlFileContent)
                    .sender("sender-test@example.com")
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(), e.getMessage());
        }
    }

    @Transactional
    public void sendUserMetaChangeNotificationSMS(String recipient, MetaOperationType metaOperationType, String lang,
                                                  String userMetaChangeId) {
        // todo implement send sms
        throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
    }

}
