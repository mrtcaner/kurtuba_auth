package com.kurtuba.auth.utils;

import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

public class ServiceUtils {

    public static void validateUserMetaChange(UserMetaChange userMetaChange, String code) {

        if (userMetaChange.getMaxTryCount() != null && userMetaChange.getTryCount() >= userMetaChange.getMaxTryCount()) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_EXPIRED);
        }

        if (StringUtils.hasLength(code) && !userMetaChange.getCode().equals(code)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH);
        }

        if (userMetaChange.isExecuted()) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_EXPIRED);
        }

        if (userMetaChange.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_EXPIRED);
        }
    }


}
