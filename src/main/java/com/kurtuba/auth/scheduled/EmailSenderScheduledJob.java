package com.kurtuba.auth.scheduled;

import com.kurtuba.auth.data.enums.EmailJobStateType;
import com.kurtuba.auth.data.model.EmailDetails;
import com.kurtuba.auth.data.model.EmailJob;
import com.kurtuba.auth.service.EmailJobService;
import com.kurtuba.auth.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EmailSenderScheduledJob {

    final
    EmailService emailService;

    final
    EmailJobService emailJobService;

    public EmailSenderScheduledJob(EmailService emailService, EmailJobService emailJobService) {
        this.emailService = emailService;
        this.emailJobService = emailJobService;
    }

    @Scheduled(fixedDelay = 5000)
    public void sendEmail() {
        List<EmailJob> jobs = emailJobService.findByStateAndSendAfterDateBefore(EmailJobStateType.PENDING, LocalDateTime.now());
        jobs.forEach(emailjob -> {
            try{
                emailService.sendMultipartMail(EmailDetails.builder()
                        .sender(emailjob.getSender())
                        .recipient(emailjob.getRecipient())
                        .subject(emailjob.getSubject())
                        .msgBody(emailjob.getMessage())
                        .build());
                emailjob.setUpdatedDate(LocalDateTime.now());
                emailjob.setTryCount(emailjob.getTryCount()+1);
                emailjob.setState(EmailJobStateType.SUCCESS);
            }catch (Exception e){
                emailjob.setError(e.getMessage());
                emailjob.setTryCount(emailjob.getTryCount()+1);
                if(emailjob.getTryCount() >= emailjob.getMaxTryCount()){
                    emailjob.setState(EmailJobStateType.FAILED);
                }
                emailjob.setUpdatedDate(LocalDateTime.now());
            }
            emailJobService.saveEmailJob(emailjob);
        });

    }

}
