package com.kurtuba.auth.service;

import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.repository.UserMetaChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserMetaChangeService {

    final
    UserMetaChangeRepository userMetaChangeRepository;

    public UserMetaChangeService(UserMetaChangeRepository userMetaChangeRepository) {
        this.userMetaChangeRepository = userMetaChangeRepository;
    }

    /**
     * deletes all non executed and same type of meta change requests of the user then creates a new one
     * there will be always one active userMetaChange per User-MetaChangeType
     *
     * @param userMetaChange
     * @return
     */
    @Transactional
    public UserMetaChange create(UserMetaChange userMetaChange){

        userMetaChangeRepository.deleteAllByExecutedIsFalseAndUserIdAndMetaOperationType(
                userMetaChange.getUserId(),userMetaChange.getMetaOperationType());
        return userMetaChangeRepository.save(userMetaChange);
    }

    @Transactional
    public void update(UserMetaChange userMetaChange){
        userMetaChangeRepository.save(userMetaChange);
    }

    public Optional<UserMetaChange> findById(String userMetaChangeId){
        return userMetaChangeRepository.findById(userMetaChangeId);
    }

    public Optional<UserMetaChange> findByLinkParam(String linkParam){
        return userMetaChangeRepository.findByLinkParam(linkParam);
    }

    /**
     * returns an active(not executed, not expired) UserMetaChange for given userId and MetaOperationType
     * @param userId
     * @return
     */
    public Optional<UserMetaChange> findActiveMetaChangeOperationForUser(String userId, MetaOperationType metaOperationType){
        return userMetaChangeRepository.findByUserIdAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(
                userId, metaOperationType, LocalDateTime.now()
        );
    }

}
