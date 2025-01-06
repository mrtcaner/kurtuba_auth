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
     * @param userMetaChange
     */
    @Transactional
    public void create(UserMetaChange userMetaChange){

        userMetaChangeRepository.deleteAllByExecutedIsFalseAndUserIdAndMetaOperationType(
                userMetaChange.getUserId(),userMetaChange.getMetaOperationType());
        userMetaChangeRepository.save(userMetaChange);
    }

    @Transactional
    public void update(UserMetaChange userMetaChange){
        userMetaChangeRepository.save(userMetaChange);
    }

    public Optional<UserMetaChange> findById(String userMetaChangeId){
        return userMetaChangeRepository.findById(userMetaChangeId);
    }

    public UserMetaChange findByLinkParam(String linkParam){
        return userMetaChangeRepository.findByLinkParam(linkParam);
    }

    /**
     * validation checks for executed, expirationDate and MetaOperationType for ACCOUNT-ACTIVATION operation
     * @param userId
     * @return
     */
    public UserMetaChange findActiveAccountActivationByUserId(String userId){
        return userMetaChangeRepository.findByUserIdAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(
                userId, MetaOperationType.ACCOUNT_ACTIVATION, LocalDateTime.now()
        );
    }

    /**
     * validation checks for executed, expirationDate and MetaOperationType for ACCOUNT-ACTIVATION operation
     * @param linkParam
     * @return
     */
    public UserMetaChange findActiveAccountActivationByLinkParam(String linkParam){
        return userMetaChangeRepository.findByLinkParamAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(
                linkParam, MetaOperationType.ACCOUNT_ACTIVATION, LocalDateTime.now()
        );
    }

    /**
     * validation checks for executed, expirationDate and MetaOperationType for PASSWORD-RESET operation
     * @param userId
     * @return
     */
    public UserMetaChange findActivePasswordResetByUserId(String userId){
        return userMetaChangeRepository.findByUserIdAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(
                userId, MetaOperationType.PASSWORD_RESET, LocalDateTime.now()
        );
    }

    /**
     * validation checks for executed, expirationDate and MetaOperationType for EMAIL-CHANGE operation
     * @param userId
     * @return
     */
    public UserMetaChange findActiveEmailChangeByUserId(String userId){
        return userMetaChangeRepository.findByUserIdAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(
                userId, MetaOperationType.EMAIL_CHANGE, LocalDateTime.now()
        );
    }

    /**
     * validation checks for executed, expirationDate and MetaOperationType for MOBILE-CHANGE operation
     * @param userId
     * @return
     */
    public UserMetaChange findActiveMobileChangeByUserId(String userId){
        return userMetaChangeRepository.findByUserIdAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(
                userId, MetaOperationType.MOBILE_CHANGE, LocalDateTime.now()
        );
    }




}
