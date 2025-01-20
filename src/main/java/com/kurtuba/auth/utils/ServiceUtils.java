package com.kurtuba.auth.utils;

import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.repository.UserRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.service.ISMSService;
import com.kurtuba.auth.service.UserMetaChangeService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
public class ServiceUtils {

    final
    UserMetaChangeService userMetaChangeService;

    final
    ISMSService smsService;

    final
    UserRepository userRepository;

    public ServiceUtils(UserMetaChangeService userMetaChangeService, ISMSService smsService, UserRepository userRepository) {
        this.userMetaChangeService = userMetaChangeService;
        this.smsService = smsService;
        this.userRepository = userRepository;
    }

    // if there is code mismatch then a transaction is required to save the result. Just for that scenario, there is
    // Propagation.REQUIRES_NEW and no roll back for BusinessLogicException
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessLogicException.class)
    public void validateUserMetaChange(UserMetaChange userMetaChange, String code) {

        if (userMetaChange.getMaxTryCount() != null && userMetaChange.getTryCount() >= userMetaChange.getMaxTryCount()) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_EXPIRED);
        }

        // for mobile, verification happens in twilio
        if (userMetaChange.getContactType().equals(ContactType.EMAIL) &&
                StringUtils.hasLength(userMetaChange.getCode()) && !userMetaChange.getCode().equals(code)) {
            updateUserMetaChangeTryCount(userMetaChange);
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH);
        }

        // call twilio endpoint
        if(userMetaChange.getContactType().equals(ContactType.MOBILE)){
            String mobile = null;
            if(userMetaChange.getMetaOperationType().equals(MetaOperationType.ACCOUNT_ACTIVATION)){
                mobile = userMetaChange.getMeta();
            }

            if(userMetaChange.getMetaOperationType().equals(MetaOperationType.MOBILE_CHANGE)){
                mobile = userMetaChange.getMeta();
            }

            if(userMetaChange.getMetaOperationType().equals(MetaOperationType.PASSWORD_RESET)){
                mobile = userRepository.getUserById(userMetaChange.getUserId()).get().getMobile();
            }

            if(mobile == null){
                throw new BusinessLogicException(ErrorEnum.GENERIC_EXCEPTION.getCode(), "Inconsistent data: userMetaChange");
            }

            boolean validationResult;
            try{
                validationResult = smsService.checkVerification(mobile, code);
            }catch (BusinessLogicException e){
                if(e.getErrorCode() == (ErrorEnum.USER_META_CHANGE_CODE_SMS_TWILIO_NOT_FOUND.getCode())){
                    updateUserMetaChangeTryCount(userMetaChange);
                }
                throw e;
            }

            if (!validationResult) {
                updateUserMetaChangeTryCount(userMetaChange);
                throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH);
            }
        }

        if (userMetaChange.isExecuted()) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_EXPIRED);
        }

        if (userMetaChange.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_EXPIRED);
        }
    }

    @Transactional
    public void updateUserMetaChangeTryCount(UserMetaChange userMetaChange) {
        userMetaChange.setTryCount(userMetaChange.getTryCount() + 1);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.update(userMetaChange);
    }


}
