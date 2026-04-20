package com.kurtuba.auth.error.exception;

import com.kurtuba.auth.error.enums.ErrorEnum;
import lombok.Getter;

@Getter
public class BusinessLogicException extends RuntimeException {

    private int errorCode;


    public BusinessLogicException(int errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;

    }

    public BusinessLogicException(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.errorCode = errorEnum.getCode();
    }

    public BusinessLogicException(int errorCode, String errorMessage, Throwable err) {
        super(errorMessage, err);
        this.errorCode = errorCode;
    }

    public BusinessLogicException(ErrorEnum errorEnum, Throwable err) {
        super(errorEnum.getMessage(), err);
        this.errorCode = errorEnum.getCode();
    }


}
