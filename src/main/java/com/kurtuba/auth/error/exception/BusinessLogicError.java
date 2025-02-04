package com.kurtuba.auth.error.exception;

import com.kurtuba.auth.error.enums.ErrorEnum;
import lombok.Data;

@Data
public class BusinessLogicError extends RuntimeException {

    private int errorCode;


    public BusinessLogicError(int errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;

    }

    public BusinessLogicError(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.errorCode = errorEnum.getCode();
    }

    public BusinessLogicError(int errorCode, String errorMessage, Throwable err) {
        super(errorMessage, err);
        this.errorCode = errorCode;
    }

    public BusinessLogicError(ErrorEnum errorEnum, Throwable err) {
        super(errorEnum.getMessage(), err);
        this.errorCode = errorEnum.getCode();
    }


}
