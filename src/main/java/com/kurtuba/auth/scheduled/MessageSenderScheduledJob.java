package com.kurtuba.auth.scheduled;

import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MessageJobStateType;
import com.kurtuba.auth.data.model.EmailDetails;
import com.kurtuba.auth.data.model.MessageJob;
import com.kurtuba.auth.service.MessageJobService;
import com.kurtuba.auth.service.EmailService;
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

    public MessageSenderScheduledJob(EmailService emailService, MessageJobService messageJobService) {
        this.emailService = emailService;
        this.messageJobService = messageJobService;
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
            messageJobService.saveEmailJob(emailJob);
        });

    }

    @Scheduled(fixedDelay = 5000)
    public void sendSMS() {
        List<MessageJob> jobs = messageJobService.findByStateAndContactTypeAndSendAfterDateBefore(MessageJobStateType.PENDING,
                ContactType.MOBILE, LocalDateTime.now());
       /* jobs.forEach(smsJob -> {
            try{
                emailService.sendMultipartMail(EmailDetails.builder()
                        .sender(smsJob.getSender())
                        .recipient(smsJob.getRecipient())
                        .subject(smsJob.getSubject())
                        .msgBody(smsJob.getMessage())
                        .build());
                smsJob.setUpdatedDate(LocalDateTime.now());
                smsJob.setTryCount(smsJob.getTryCount()+1);
                smsJob.setState(MessageJobStateType.SUCCESS);
            }catch (Exception e){
                smsJob.setError(e.getMessage());
                smsJob.setTryCount(smsJob.getTryCount()+1);
                if(smsJob.getTryCount() >= smsJob.getMaxTryCount()){
                    smsJob.setState(MessageJobStateType.FAILED);
                }
                smsJob.setUpdatedDate(LocalDateTime.now());
            }
            messageJobService.saveEmailJob(smsJob);
        });*/

    }

}
