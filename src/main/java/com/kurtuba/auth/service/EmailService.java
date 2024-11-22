
package com.kurtuba.auth.service;


import com.kurtuba.auth.data.model.EmailDetails;
import jakarta.validation.constraints.NotEmpty;


public interface EmailService {

    String sendSimpleMail(EmailDetails details);

    void sendValidationCodeMail(@NotEmpty String recipient, @NotEmpty String verificationCode);

    String sendMailWithAttachment(EmailDetails details);
}
