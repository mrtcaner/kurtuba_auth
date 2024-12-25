package com.kurtuba.auth.error.enums;

public enum ErrorEnum {

    GENERIC_EXCEPTION("Error",1000),
    INVALID_DATA("Invalid Data",1001),
    RESOURCE_NOT_FOUND("Resource not found",1002),
    LOGIN_INVALID_CREDENTIALS("Invalid credentials",1003),
    LOGIN_USER_LOCKED("Account locked",1004),
    USER_EMAIL_VALIDATION_STATUS_INVALID("Email already validated or user doesn't exist",1005),
    USER_EMAIL_VALIDATION_CODE_INVALID("Wrong email validation code",1006),
    USER_EMAIL_VALIDATE_CODE_EXPIRED("Email validation code expired",1007),
    MAIL_UNABLE_TO_SEND("Unable to send mail",1008),
    USER_EMAIL_ALREADY_EXISTS("A user with email address already exists",1009),
    USER_USERNAME_ALREADY_EXISTS("A user with username already exists",1010),
    USER_OTHER_PROVIDER_INVALID_TOKEN("Invalid provider token",1011),
    AUTH_REFRESH_TOKEN_INVALID("Invalid refresh token",1012),
    USER_INVALID_STATE("Invalid user state",1013),
    USER_PASSWORD_CHANGE_WRONG_PASSWORD("Old password wrong",1014),
    USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH("New passwords don't match",1015),
    USER_PASSWORD_RESET_CODE_INVALID("Password reset code invalid",1016),
    USER_PASSWORD_RESET_CODE_EXPIRED("Password reset code expired",1017),
    USER_PASSWORD_RESET_EMAIL_NOT_VALIDATED("User email is not validated",1018),
    AUTH_INVALID_TOKEN("Invalid token",1019),
    AUTH_CLIENT_INVALID_CREDENTIALS("Invalid client credentials",1020),
    AUTH_CLIENT_INVALID("Invalid client",1021);


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
