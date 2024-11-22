package com.kurtuba.auth.error.enums;

public enum ErrorEnum {

    GENERIC_EXCEPTION("Error",1000),
    INVALID_DATA("Invalid Data",1001),
    RESOURCE_NOT_FOUND("Resource not found",1002),
    LOGIN_INVALID_CREDENTIALS("Invalid credentials",1003),
    LOGIN_USER_LOCKED("Account locked",1004),
    USER_EMAIL_VALIDATION_STATUS_INVALID("User email already validated or user doesn't exist",1005),
    USER_EMAIL_VALIDATION_CODE_INVALID("Wrong email validation code",1006),
    MAIL_UNABLE_TO_SEND("Unable to send mail",1007),
    USER_EMAIL_ALREADY_EXISTS("A user with email address already exists",1008),
    USER_USERNAME_ALREADY_EXISTS("A user with username already exists",1009),
    USER_OTHER_PROVIDER_INVALID_TOKEN("Invalid provider token",1010);

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
