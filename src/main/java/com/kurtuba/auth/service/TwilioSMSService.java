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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;


@Service
@Profile("!local")
public class TwilioSMSService implements ISMSService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioSMSService.class);

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

        Message message = Message.creator(new PhoneNumber(recipientNumber), new PhoneNumber(sender), messageContent)
                                 .create();

        /*Message message = Message
                .creator(new PhoneNumber(recipientNumber), MESSAGING_SERVICE_SID, messageContent).create();*/
        return message;
        //sendVerificationSMS(recipientNumber);
    }

    public Verification sendVerificationSMS(String recipient) {
        try {
            Verification verification = Verification.creator(VERIFY_SERVICE_SID, recipient, "sms").create();
            LOGGER.info("Twilio verification created with sid {} for recipient {}", verification.getSid(), recipient);

            return verification;
        } catch (Exception e) {
            LOGGER.error("Twilio verification creation failed for recipient {}", recipient, e);
            if (e instanceof ApiException) {
                ApiException apiException = (ApiException) e;
                if (apiException.getCode().equals(60203)) {
                    //  Max check attempts reached, status 429
                    throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_TWILIO_TOO_MANY_RESEND, e);
                } else if (apiException.getCode().equals(20003)) {
                    //  Invalid credentials
                    throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_TWILIO_AUTHENTICATION_ERROR, e);
                }
            }
        }
        throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_UNEXPECTED_ERROR);
    }

    public Boolean checkVerification(String userMobile, String code) {
        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(VERIFY_SERVICE_SID)
                                                                   .setTo(userMobile)
                                                                   .setCode(code)
                                                                   .create();
            LOGGER.info("Twilio verification check completed for mobile {} with status {}", userMobile,
                    verificationCheck.getStatus());

            return verificationCheck.getValid();
        } catch (Exception e) {
            LOGGER.error("Twilio verification check failed for mobile {}", userMobile, e);
            if (e instanceof ApiException && ((ApiException) e).getCode().equals(60202)) {
                //  Max check attempts reached, status 429
                throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_EXPIRED, e);
            }

            if (e instanceof ApiException && ((ApiException) e).getStatusCode().equals(HttpStatus.NOT_FOUND_404)) {
                throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_TWILIO_NOT_FOUND, e);
            }

            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_UNEXPECTED_ERROR, e);
        }
    }

    @Override
    public Object deleteVerification(String sid) {
        try {
            return Verification.updater(VERIFY_SERVICE_SID, sid, Verification.Status.CANCELED).update();
        } catch (Exception e) {
            LOGGER.error("Twilio verification delete failed for sid {}", sid, e);
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_SMS_UNEXPECTED_ERROR, e);
        }
    }


}
