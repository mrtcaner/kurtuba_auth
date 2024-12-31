
package com.kurtuba.auth.service;


import com.kurtuba.auth.data.enums.MetaChangeType;
import com.kurtuba.auth.data.model.EmailDetails;
import jakarta.validation.constraints.NotEmpty;


public interface EmailService {

    void sendUserMetaChangeNotificationMail(String recipient, MetaChangeType metaChangeType);

    void sendMultipartMail(EmailDetails details);

    String sendSimpleMail(EmailDetails details);

    String sendMailWithAttachment(EmailDetails details);

    void sendRegistrationValidationCodeMail(@NotEmpty String recipient, @NotEmpty String verificationCode);

    void sendRegistrationValidationLinkMail(@NotEmpty String recipient, @NotEmpty String verificationLink);

    void sendUserEmailChangeCodeMail(@NotEmpty String recipient, @NotEmpty String verificationCode);

    void sendUserEmailChangeLinkMail(@NotEmpty String recipient, @NotEmpty String verificationCode);

    void sendPasswordResetCodeMail(@NotEmpty String recipient, @NotEmpty String resetCode);

    void sendPasswordResetLinkMail(@NotEmpty String recipient, @NotEmpty String resetCode);
}
