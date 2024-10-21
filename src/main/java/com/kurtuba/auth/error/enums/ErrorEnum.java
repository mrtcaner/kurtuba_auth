package com.parafusion.auth.error.enums;

public enum ErrorEnum {

    GENERIC_EXCEPTION("Error",1000),
    INVALID_DATA("Invalid Data",1001),
    RESOURCE_NOT_FOUND("Resource not found",1002),
    LOGIN_INVALID_CREDENTIALS("Invalid credentials",1003),
    LOGIN_USER_LOCKED("Account locked",1003);

    private String message;
    private Integer code;

    ErrorEnum(String message, int code){
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public Integer getCode() {
        return code;
    }
}
