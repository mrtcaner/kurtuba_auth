package com.kurtuba.auth.scheduled;

import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MessageJobStateType;
import com.kurtuba.auth.data.enums.MessageServiceProviderType;
import com.kurtuba.auth.data.model.EmailDetails;
import com.kurtuba.auth.data.model.MessageJob;
import com.kurtuba.auth.service.ISMSService;
import com.kurtuba.auth.service.MessageJobService;
import com.kurtuba.auth.service.EmailService;
import com.twilio.rest.verify.v2.service.Verification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MessageSenderScheduledJob {

    final
    EmailService emailService;

    final
    MessageJobService messageJobService;

    final
    ISMSService smsService;

    public MessageSenderScheduledJob(EmailService emailService, MessageJobService messageJobService, ISMSService smsService) {
        this.emailService = emailService;
        this.messageJobService = messageJobService;
        this.smsService = smsService;
    }

    @Scheduled(fixedDelay = 5000)
    public void sendEmail() {
        List<MessageJob> jobs = messageJobService.findByStateAndContactTypeAndSendAfterDateBefore(MessageJobStateType.PENDING,
                ContactType.EMAIL, LocalDateTime.now());
        jobs.forEach(emailJob -> {
            try{
                emailService.sendMultipartMail(EmailDetails.builder()
                        .sender(emailJob.getSender())
                        .recipient(emailJob.getRecipient())
                        .subject(emailJob.getSubject())
                        .msgBody(emailJob.getMessage())
                        .build());
                emailJob.setUpdatedDate(LocalDateTime.now());
                emailJob.setTryCount(emailJob.getTryCount()+1);
                emailJob.setState(MessageJobStateType.SUCCESS);
            }catch (Exception e){
                emailJob.setError(e.getMessage());
                emailJob.setTryCount(emailJob.getTryCount()+1);
                if(emailJob.getTryCount() >= emailJob.getMaxTryCount()){
                    emailJob.setState(MessageJobStateType.FAILED);
                }
                emailJob.setUpdatedDate(LocalDateTime.now());
            }
            messageJobService.saveMessageJob(emailJob);
        });

    }

    /*@Scheduled(fixedDelay = 5000)
    public void sendSMS() {
    }*/

    @Scheduled(fixedDelay = 5000)
    public void sendTwilioVerificationSMS() {
        List<MessageJob> jobs = messageJobService
                .findByStateAndContactTypeAndMessageServiceProviderTypeAndSendAfterDateBefore(MessageJobStateType.PENDING,
                ContactType.MOBILE, MessageServiceProviderType.TWILIO_VERIFY, LocalDateTime.now());
        jobs.forEach(smsJob -> {
            try{
                Verification verification = smsService.sendVerificationSMS(smsJob.getRecipient());
                smsJob.setSid(verification.getSid());
                smsJob.setUpdatedDate(LocalDateTime.now());
                smsJob.setTryCount(smsJob.getTryCount()+1);
                smsJob.setState(MessageJobStateType.SUCCESS);
                messageJobService.saveMessageJob(smsJob);

            }catch (Exception e){
                smsJob.setError(e.getMessage());
                smsJob.setTryCount(smsJob.getTryCount()+1);
                // stays in pending state as long as maxTryCount reached
                if(smsJob.getTryCount() >= smsJob.getMaxTryCount()){
                    smsJob.setState(MessageJobStateType.FAILED);
                }
                smsJob.setUpdatedDate(LocalDateTime.now());
            }
            messageJobService.saveMessageJob(smsJob);
        });

    }

}
