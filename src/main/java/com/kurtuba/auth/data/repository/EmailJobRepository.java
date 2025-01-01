package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.EmailJobStateType;
import com.kurtuba.auth.data.model.EmailJob;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailJobRepository  extends CrudRepository<EmailJob, String> {

    @Query("SELECT job FROM EmailJob job WHERE job.state = :state and job.sendAfterDate <= :before order by job.createdDate limit 50")
    List<EmailJob> findByStateAndSendAfterDateBefore(EmailJobStateType state, LocalDateTime before);
}
