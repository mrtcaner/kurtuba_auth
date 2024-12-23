
package com.kurtuba.auth.service;


import com.kurtuba.auth.data.model.EmailDetails;
import jakarta.validation.constraints.NotEmpty;


public interface EmailService {

    String sendSimpleMail(EmailDetails details);

    String sendMailWithAttachment(EmailDetails details);

    void sendValidationCodeMail(@NotEmpty String recipient, @NotEmpty String verificationCode);

    void sendValidationLinkMail(@NotEmpty String recipient, @NotEmpty String verificationLink);

    void sendPasswordResetCodeMail(@NotEmpty String recipient, @NotEmpty String resetCode);

    void sendPasswordResetLinkMail(@NotEmpty String recipient, @NotEmpty String resetCode);

    void sendUserMetaChangeMail(@NotEmpty String recipient, @NotEmpty String meta);
}
