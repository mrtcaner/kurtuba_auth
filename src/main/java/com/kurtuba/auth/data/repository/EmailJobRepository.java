package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.EmailJobStateType;
import com.kurtuba.auth.data.model.EmailJob;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailJobRepository  extends CrudRepository<EmailJob, String> {

    List<EmailJob> findByStateAndSendAfterDateBefore(EmailJobStateType state, LocalDateTime before);
}
