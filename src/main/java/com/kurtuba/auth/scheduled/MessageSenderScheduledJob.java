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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "kurtuba.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageSenderScheduledJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSenderScheduledJob.class);

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
                ContactType.EMAIL, Instant.now());
        LOGGER.debug("Email message job fetched jobs. jobCount={}", jobs.size());
        jobs.forEach(emailJob -> {
            try{
                emailService.sendMultipartMail(EmailDetails.builder()
                        .sender(emailJob.getSender())
                        .recipient(emailJob.getRecipient())
                        .subject(emailJob.getSubject())
                        .msgBody(emailJob.getMessage())
                        .build());
                emailJob.setUpdatedDate(Instant.now());
                emailJob.setTryCount(emailJob.getTryCount()+1);
                emailJob.setState(MessageJobStateType.SUCCESS);
            }catch (Exception e){
                LOGGER.error("Email message job failed. jobId={}, recipient={}, tryCount={}, maxTryCount={}",
                        emailJob.getId(), emailJob.getRecipient(), emailJob.getTryCount(), emailJob.getMaxTryCount(), e);
                emailJob.setError(e.getMessage());
                emailJob.setTryCount(emailJob.getTryCount()+1);
                if(emailJob.getTryCount() >= emailJob.getMaxTryCount()){
                    emailJob.setState(MessageJobStateType.FAILED);
                }
                emailJob.setUpdatedDate(Instant.now());
            }
            messageJobService.saveMessageJob(emailJob);
        });

    }

    @Scheduled(fixedDelay = 5000)
    public void sendTwilioVerificationSMS() {
        List<MessageJob> jobs = messageJobService
                .findByStateAndContactTypeAndMessageServiceProviderTypeAndSendAfterDateBefore(MessageJobStateType.PENDING,
                ContactType.MOBILE, MessageServiceProviderType.TWILIO_VERIFY, Instant.now());
        LOGGER.debug("Twilio verification message job fetched jobs. jobCount={}", jobs.size());
        jobs.forEach(smsJob -> {
            try{
                Verification verification = smsService.sendVerificationSMS(smsJob.getRecipient());
                smsJob.setSid(verification.getSid());
                smsJob.setUpdatedDate(Instant.now());
                smsJob.setTryCount(smsJob.getTryCount()+1);
                smsJob.setState(MessageJobStateType.SUCCESS);
                messageJobService.saveMessageJob(smsJob);

            }catch (Exception e){
                LOGGER.error("Twilio verification message job failed. jobId={}, recipient={}, tryCount={}, maxTryCount={}",
                        smsJob.getId(), smsJob.getRecipient(), smsJob.getTryCount(), smsJob.getMaxTryCount(), e);
                smsJob.setError(e.getMessage());
                smsJob.setTryCount(smsJob.getTryCount()+1);
                // stays in pending state as long as maxTryCount reached
                if(smsJob.getTryCount() >= smsJob.getMaxTryCount()){
                    smsJob.setState(MessageJobStateType.FAILED);
                }
                smsJob.setUpdatedDate(Instant.now());
            }
            messageJobService.saveMessageJob(smsJob);
        });

    }

}
