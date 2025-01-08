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
    USER_CONTACT_REQUIRED("An email address or a mobile number must be provided",1302),
    USER_EMAIL_ALREADY_EXISTS("A user with email address already exists",1303),
    USER_INVALID_STATE("Invalid user state",1304),
    USER_OTHER_PROVIDER_INVALID_TOKEN("Invalid provider token",1305),
    USER_EMAIL_NOT_VERIFIED("User email is not verified",1306),
    USER_EMAIL_VERIFICATION_STATUS_INVALID("Email already verified or user doesn't exist",1307),
    USER_PASSWORD_CHANGE_WRONG_PASSWORD("Old password wrong",1310),
    USER_MOBILE_ALREADY_EXISTS("A user with mobile number already exists",1311),
    USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH("New passwords don't match",1312),
    USER_META_CHANGE_INVALID_OPERATION("Invalid meta modification. Operation carried out or doesn't exist",1315),
    USER_META_CHANGE_CODE_MISMATCH("Code mismatch",1316),
    USER_META_CHANGE_CODE_EXPIRED("Code expired",1317),
    USER_REGISTRATION_CONTACT_TYPE_MAIL_MISSING("email contact is preferred but no email is provided",1318),
    USER_REGISTRATION_CONTACT_TYPE_MOBILE_MISSING("Mobile contact is preferred but no number is provided",1319),
    USER_MOBILE_NOT_VERIFIED("User mobile is not verified",1320),
    MAIL_UNABLE_TO_SEND("Unable to send mail",1400),
    LOCALIZATION_INVALID_RESOURCE_ID("Invalid resource id",1500);



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
