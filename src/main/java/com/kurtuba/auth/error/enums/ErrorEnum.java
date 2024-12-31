package com.kurtuba.auth.error.enums;

public enum ErrorEnum {

    GENERIC_EXCEPTION("Error",1000),
    INVALID_PARAMETER("Validation failure",1001),
    RESOURCE_NOT_FOUND("Resource not found",1002),
    AUTH_INVALID_TOKEN("Invalid token",1100),
    AUTH_REFRESH_TOKEN_INVALID("Invalid refresh token",1101),
    AUTH_CLIENT_INVALID("Invalid client",1102),
    AUTH_CLIENT_INVALID_CREDENTIALS("Invalid client credentials",1103),
    LOGIN_INVALID_CREDENTIALS("Invalid credentials",1200),
    LOGIN_USER_LOCKED("Account locked",1201),
    USER_DOESNT_EXIST("User doesn't exist", 1300),
    USER_USERNAME_ALREADY_EXISTS("A user with username already exists",1301),
    USER_EMAIL_ALREADY_EXISTS("A user with email address already exists",1302),
    USER_INVALID_STATE("Invalid user state",1303),
    USER_OTHER_PROVIDER_INVALID_TOKEN("Invalid provider token",1304),
    USER_EMAIL_NOT_VALIDATED("User email is not validated",1305),
    USER_EMAIL_VALIDATION_STATUS_INVALID("Email already validated or user doesn't exist",1306),
    USER_EMAIL_VALIDATION_CODE_INVALID("Wrong email validation code",1307),
    USER_EMAIL_VALIDATE_CODE_EXPIRED("Email validation code expired",1308),
    USER_PASSWORD_CHANGE_WRONG_PASSWORD("Old password wrong",1309),
    USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH("New passwords don't match",1310),
    USER_PASSWORD_RESET_CODE_INVALID("Password reset code invalid",1311),
    USER_PASSWORD_RESET_CODE_EXPIRED("Password reset code expired",1312),
    MAIL_UNABLE_TO_SEND("Unable to send mail",1400);


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
