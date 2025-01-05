package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MessageJobStateType;
import com.kurtuba.auth.data.model.MessageJob;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageJobRepository extends CrudRepository<MessageJob, String> {

    @Query("SELECT job FROM MessageJob job WHERE job.state = :state and job.sendAfterDate <= :before " +
            "order by job.createdDate limit 50")
    List<MessageJob> findByStateAndSendAfterDateBefore(MessageJobStateType state, LocalDateTime before);

    @Query("SELECT job FROM MessageJob job WHERE job.state = :state and job.contactType = :contactType" +
            " and job.sendAfterDate <= :before order by job.createdDate limit 50")
    List<MessageJob> findByStateAndContactTypeAndSendAfterDateBefore(MessageJobStateType state, ContactType contactType,
                                                                     LocalDateTime before);
}
