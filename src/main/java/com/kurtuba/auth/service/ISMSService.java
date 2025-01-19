package com.kurtuba.auth.service;


import com.twilio.rest.verify.v2.service.Verification;


public interface ISMSService {

    Object sendSMS(String recipientNumber, String sender, String messageContent);

    Verification sendVerificationSMS(String recipient);

    Boolean checkVerification(String userMobile, String code);

}
