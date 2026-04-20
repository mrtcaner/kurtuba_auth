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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class MessageJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageJobService.class);

    @Value("${kurtuba.server.url}")
    private String kurtubaServerUrl;

    @Value("${kurtuba.mail.from-address}")
    private String fromAddress;

    @Value("${kurtuba.mail.support-address}")
    private String supportAddress;

    @Value("${kurtuba.job.email.send.max-try-count}")
    private String emailSendMaxTryCount;

    @Value("${kurtuba.job.sms.send.max-try-count}")
    private String smsSendMaxTryCount;

    private final MessageJobRepository messageJobRepository;

    private final LocalizationMessageService localizationMessageService;


    @Transactional
    public void saveMessageJob(MessageJob messageJob) {
        messageJobRepository.save(messageJob);
    }

    public List<MessageJob> findByStateAndSendAfterDateBefore(MessageJobStateType jobState, Instant before) {
        return messageJobRepository.findByStateAndSendAfterDateBefore(jobState, before);
    }

    public List<MessageJob> findByStateAndContactTypeAndSendAfterDateBefore(MessageJobStateType jobState,
                                                                            ContactType contactType,
                                                                            Instant before) {
        return messageJobRepository.findByStateAndContactTypeAndSendAfterDateBefore(jobState, contactType, before);
    }

    public List<MessageJob> findByStateAndContactTypeAndMessageServiceProviderTypeAndSendAfterDateBefore(MessageJobStateType jobState,
                                                                                                         ContactType contactType, MessageServiceProviderType serviceProvider, Instant before) {
        return messageJobRepository.findByStateAndContactTypeAndMessageServiceProviderTypeAndSendAfterDateBefore(jobState, contactType, serviceProvider, before);
    }

    @Transactional
    public void sendAccountActivationCodeMail(@NotBlank String recipient, @NotBlank String verificationCode,
                                              @NotBlank String lang, String userMetaChangeId) {

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation.content" +
                                                                                        ".title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation.content" +
                                                                                        ".greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation.content" +
                                                                                           ".code.prologue").getMessage())
                .verificationCode(verificationCode)
                .verificationLink("")
                .verifyEmailBtnLabel("")
                .displayCode("block")
                .displayLink("none")
                .epilogue(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation.content" +
                                                                                           ".code.epilogue").getMessage())
                .closing(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation.content" +
                                                                                          ".closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation" +
                                                                                                 ".content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation" +
                                                                                             ".content.get-in-touch").getMessage())
                .supportEmail(supportAddress)
                .build();
        createAccountActivationMessageJob(recipient, lang, verificationMailDto, userMetaChangeId);
    }

    private void createAccountActivationMessageJob(@NotBlank String recipient, @NotBlank String lang,
                                                   EmailVerificationMailDto verificationMailDto,
                                                   String userMetaChangeId) {
        try {
            String htmlFileContent = EmailUtils.setEmailVerificationMessageBody(verificationMailDto);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(Instant.now())
                    .contactType(ContactType.EMAIL)
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(Instant.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService
                                     .findByLanguageCodeAndMessageKey(lang, "mail.account.activation.subject").getMessage())
                    .message(htmlFileContent)
                    .sender(fromAddress)
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw logAndWrapMailFailure("create account activation mail job", recipient, lang, userMetaChangeId, e);
        }
    }

    @Transactional
    public void sendVerificationCodeSMSViaTwilio(@MobileNumber String mobileNumber, String userMetaChangeId) {
        messageJobRepository.save(MessageJob.builder()
                .createdDate(Instant.now())
                .contactType(ContactType.MOBILE)
                .serviceProvider(MessageServiceProviderType.TWILIO_VERIFY)
                .maxTryCount(Integer.parseInt(smsSendMaxTryCount))
                .sendAfterDate(Instant.now())
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

        String verificationLink = kurtubaServerUrl + "/auth/registration/activation/link/" + verificationCode;

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation.content" +
                                                                                        ".title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation.content" +
                                                                                        ".greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation.content" +
                                                                                           ".link.prologue").getMessage())
                .verificationLink(verificationLink)
                .verificationCode("")
                .verifyEmailBtnLabel(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account" +
                                                                                                      ".activation.content.link.button.label").getMessage())
                .displayCode("none")
                .displayLink("block")
                .epilogue("")
                .closing(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation.content" +
                                                                                          ".closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation" +
                                                                                                 ".content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.activation" +
                                                                                             ".content.get-in-touch").getMessage())
                .supportEmail(supportAddress)
                .build();

        createAccountActivationMessageJob(recipient, lang, verificationMailDto, userMetaChangeId);
    }

    @Transactional
    public void sendPasswordResetCodeMail(@NotBlank String recipient, @NotBlank String resetCode, String lang,
                                          String userMetaChangeId) {

        try {
            String htmlFileContent = EmailUtils.loadTemplate("templates/mailPasswordReset.html");
            htmlFileContent = htmlFileContent.replace("${resetCode}", resetCode);
            htmlFileContent = htmlFileContent.replace("${displayCode}", "block");
            htmlFileContent = htmlFileContent.replace("${displayLink}", "none");
            htmlFileContent = htmlFileContent.replace("${prologue}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.code.prologue").getMessage());
            htmlFileContent = htmlFileContent.replace("${epilogue}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.epilogue").getMessage());
            htmlFileContent = htmlFileContent.replace("${closing}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.closing").getMessage());
            htmlFileContent = htmlFileContent.replace("${closingSubject}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.closing.subject").getMessage());
            htmlFileContent = htmlFileContent.replace("${getInTouch}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.get-in-touch").getMessage());
            htmlFileContent = htmlFileContent.replace("${supportEmail}", supportAddress);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(Instant.now())
                    .contactType(ContactType.EMAIL)
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(Instant.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.password.reset.subject").getMessage())
                    .message(htmlFileContent)
                    .sender(fromAddress)
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw logAndWrapMailFailure("create password reset mail job", recipient, lang, userMetaChangeId, e);
        }

    }

    @Transactional
    public void sendPasswordResetLinkMail(String recipient, String resetCode, String lang, String userMetaChangeId) {

        String resetLink = kurtubaServerUrl + "/auth/user/password/reset/password-reset/" + resetCode;
        try {
            String htmlFileContent = EmailUtils.loadTemplate("templates/mailPasswordReset.html");
            htmlFileContent = htmlFileContent.replace("${resetLink}", resetLink);
            htmlFileContent = htmlFileContent.replace("${displayLink}", "block");
            htmlFileContent = htmlFileContent.replace("${displayCode}", "none");
            htmlFileContent = htmlFileContent.replace("${prologue}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.link.prologue").getMessage());
            htmlFileContent = htmlFileContent.replace("${epilogue}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.epilogue").getMessage());
            htmlFileContent = htmlFileContent.replace("${closing}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.closing").getMessage());
            htmlFileContent = htmlFileContent.replace("${closingSubject}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.closing.subject").getMessage());
            htmlFileContent = htmlFileContent.replace("${getInTouch}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.password.reset.content.get-in-touch").getMessage());
            htmlFileContent = htmlFileContent.replace("${supportEmail}", supportAddress);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(Instant.now())
                    .contactType(ContactType.EMAIL)
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(Instant.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.password.reset.subject").getMessage())
                    .message(htmlFileContent)
                    .sender(fromAddress)
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw logAndWrapMailFailure("create password reset link mail job", recipient, lang, userMetaChangeId, e);
        }
    }

    @Transactional
    public void sendUserEmailChangeCodeMail(String recipient, String verificationCode, String lang,
                                            String userMetaChangeId) {

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification.content" +
                                                                                        ".title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification.content" +
                                                                                        ".greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification.content" +
                                                                                           ".code.prologue").getMessage())
                .verificationCode(verificationCode)
                .verificationLink("")
                .verifyEmailBtnLabel("")
                .displayCode("block")
                .displayLink("none")
                .epilogue("")
                .closing(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification.content" +
                                                                                          ".closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification" +
                                                                                                 ".content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification" +
                                                                                             ".content.get-in-touch").getMessage())
                .supportEmail(supportAddress)
                .build();

        createEmailChangeMessageJob(recipient, lang, verificationMailDto, userMetaChangeId);
    }

    private void createEmailChangeMessageJob(String recipient, String lang,
                                             EmailVerificationMailDto verificationMailDto, String userMetaChangeId) {
        try {
            String htmlFileContent = EmailUtils.setEmailVerificationMessageBody(verificationMailDto);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(Instant.now())
                    .contactType(ContactType.EMAIL)
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(Instant.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification" +
                                                                                              ".subject").getMessage())
                    .message(htmlFileContent)
                    .sender(fromAddress)
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw logAndWrapMailFailure("create email change verification mail job", recipient, lang, userMetaChangeId, e);
        }
    }

    @Transactional
    public void sendUserEmailChangeLinkMail(String recipient, String verificationCode, String lang,
                                            String userMetaChangeId) {

        String verificationLink = kurtubaServerUrl + "/auth/user/email/verification/link/" + verificationCode;

        EmailVerificationMailDto verificationMailDto = EmailVerificationMailDto.builder()
                .title(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification.content" +
                                                                                        ".title").getMessage())
                .greet(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification.content" +
                                                                                        ".greet").getMessage())
                .prologue(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification.content" +
                                                                                           ".link.prologue").getMessage())
                .verificationLink(verificationLink)
                .verifyEmailBtnLabel(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email" +
                                                                                                      ".verification.content.link.button.label").getMessage())
                .verificationCode("")
                .displayCode("none")
                .displayLink("block")
                .epilogue("")
                .closing(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification.content" +
                                                                                          ".closing").getMessage())
                .closingSubject(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification" +
                                                                                                 ".content.closing.subject").getMessage())
                .getInTouch(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.email.verification" +
                                                                                             ".content.get-in-touch").getMessage())
                .supportEmail(supportAddress)
                .build();

        createEmailChangeMessageJob(recipient, lang, verificationMailDto, userMetaChangeId);
    }

    @Transactional
    public void sendUserMetaChangeNotificationMail(String recipient, MetaOperationType metaOperationType, String lang,
                                                   String userMetaChangeId) {

        String metaName =
                metaOperationType == MetaOperationType.PASSWORD_CHANGE ||
                        metaOperationType == MetaOperationType.PASSWORD_RESET ? localizationMessageService
                        .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.metaname.password").getMessage()
                        : metaOperationType == MetaOperationType.MOBILE_CHANGE ? localizationMessageService
                                .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.metaname" +
                                                                       ".phonenumber").getMessage()
                        : localizationMessageService
                                  .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.metaname" +
                                                                         ".emailaddress").getMessage();
        try {
            String htmlFileContent = EmailUtils.loadTemplate("templates/mailUserMetaChangeNotification.html");
            htmlFileContent = htmlFileContent.replace("${greet}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.greet").getMessage());
            htmlFileContent = htmlFileContent.replace("${prologue}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.prologue").getMessage());
            htmlFileContent = htmlFileContent.replace("${context}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.context").getMessage());
            htmlFileContent = htmlFileContent.replace("${epilogue}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.epilogue").getMessage());
            htmlFileContent = htmlFileContent.replace("${closing}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.closing").getMessage());
            htmlFileContent = htmlFileContent.replace("${closingSubject}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.closing.subject").getMessage());
            htmlFileContent = htmlFileContent.replace("${getInTouch}", localizationMessageService
                    .findByLanguageCodeAndMessageKey(lang, "mail.account.modification.content.get-in-touch").getMessage());
            htmlFileContent = htmlFileContent.replace("${supportEmail}", supportAddress);
            htmlFileContent = htmlFileContent.replaceAll("metaName", metaName);

            messageJobRepository.save(MessageJob.builder()
                    .createdDate(Instant.now())
                    .contactType(ContactType.EMAIL)
                    .serviceProvider(MessageServiceProviderType.KURTUBA)
                    .maxTryCount(Integer.parseInt(emailSendMaxTryCount))
                    .sendAfterDate(Instant.now())
                    .state(MessageJobStateType.PENDING)
                    .tryCount(0)
                    .recipient(recipient)
                    .subject(localizationMessageService.findByLanguageCodeAndMessageKey(lang, "mail.account.modification" +
                                                                                              ".subject").getMessage())
                    .message(htmlFileContent)
                    .sender(fromAddress)
                    .userMetaChangeId(userMetaChangeId)
                    .build());
        } catch (Exception e) {
            throw logAndWrapMailFailure("create account modification notification mail job", recipient, lang,
                    userMetaChangeId, e);
        }
    }

    @Transactional
    public void sendUserMetaChangeNotificationSMS(String recipient, MetaOperationType metaOperationType, String lang,
                                                  String userMetaChangeId) {
        // todo implement send sms
        throw new NotImplementedException("Feature incomplete. Contact assistance.");
    }

    public MessageJobStateType findByUserMetaChangeIdAndUserId(String userMetaChangeId, String userId) {
        MessageJob job =
                messageJobRepository.findByUserMetaChangeIdAndUserId(userMetaChangeId, userId)
                                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.RESOURCE_NOT_FOUND));
        return job.getState();
    }

    private BusinessLogicException logAndWrapMailFailure(String operation, String recipient, String lang,
                                                         String userMetaChangeId, Exception cause) {
        String causeMessage = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        LOGGER.error("Failed to {}. recipient={}, lang={}, userMetaChangeId={}, cause={}",
                operation, recipient, lang, userMetaChangeId, causeMessage, cause);
        return new BusinessLogicException(ErrorEnum.MAIL_UNABLE_TO_SEND.getCode(),
                ErrorEnum.MAIL_UNABLE_TO_SEND.getMessage() + ": " + causeMessage, cause);
    }
}
