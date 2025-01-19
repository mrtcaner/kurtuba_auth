package com.kurtuba.auth.service;

import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class TwilioSMSService implements ISMSService{

    @Value("${kurtuba.twilio.us.live.sid}")
    private String ACCOUNT_SID;
    @Value("${kurtuba.twilio.us.live.auth-token}")
    private String AUTH_TOKEN;

    @Value("${kurtuba.twilio.messaging-service.sid}")
    private String MESSAGING_SERVICE_SID;

    @Value("${kurtuba.twilio.verify-service.sid}")
    private String VERIFY_SERVICE_SID;

    private com.twilio.rest.messaging.v1.Service twilioService;

    @PostConstruct
    void init() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        //twilioService = com.twilio.rest.messaging.v1.Service.creator("KURTUBA").create();
    }

    public Message sendSMS(String recipientNumber, String sender, String messageContent) {

        Message message = Message
                .creator(new PhoneNumber(recipientNumber), new PhoneNumber(sender), messageContent).create();

        /*Message message = Message
                .creator(new PhoneNumber(recipientNumber), MESSAGING_SERVICE_SID, messageContent).create();*/
        return message;
        //sendVerificationSMS(recipientNumber);
    }

    public Verification sendVerificationSMS(String recipient) {
        try {
            Verification verification = Verification.creator(
                            VERIFY_SERVICE_SID,
                            recipient,
                            "sms")
                    .create();

            System.out.println(verification.getSid());
            System.out.println(verification);

            return verification;
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof ApiException && ((ApiException) e).getCode().equals(60203)) {
                //  Max check attempts reached, status 429
                throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_TWILIO_TOO_MANY_RESEND);
            }
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_UNEXPECTED_ERROR);
        }
    }

    public Boolean checkVerification(String userMobile, String code) {
        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(
                            VERIFY_SERVICE_SID)
                    .setTo(userMobile)
                    .setCode(code)
                    .create();
            System.out.println(verificationCheck);

            return verificationCheck.getValid();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof ApiException && ((ApiException) e).getCode().equals(60202)) {
                //  Max check attempts reached, status 429
                throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_EXPIRED);
            }

            if (e instanceof ApiException && ((ApiException) e).getStatusCode().equals(HttpStatus.NOT_FOUND_404)) {
                throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_TWILIO_NOT_FOUND);
            }

            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_UNEXPECTED_ERROR);
        }
    }


}
