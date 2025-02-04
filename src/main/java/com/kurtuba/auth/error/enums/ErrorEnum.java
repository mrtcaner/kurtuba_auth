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
    USER_PASSWORD_CHANGE_WRONG_OLD_PASSWORD("Old password wrong",1310),
    USER_MOBILE_ALREADY_EXISTS("A user with mobile number already exists",1311),
    USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH("New passwords don't match",1312),
    USER_META_CHANGE_INVALID_OPERATION("Operation carried out or doesn't exist",1315),
    USER_META_CHANGE_CODE_MISMATCH("Code mismatch",1316),
    USER_META_CHANGE_CODE_EXPIRED("Code expired",1317),
    USER_REGISTRATION_CONTACT_TYPE_MAIL_MISSING("email contact is preferred but no email is provided",1318),
    USER_REGISTRATION_CONTACT_TYPE_MOBILE_MISSING("Mobile contact is preferred but no number is provided",1319),
    USER_MOBILE_NOT_VERIFIED("User mobile is not verified",1320),
    USER_ACTIVATION_NOT_ACTIVATED("Account not activated",1321),
    USER_META_CHANGE_CODE_SMS_TWILIO_TOO_MANY_RESEND("Too many SMS resend calls",1322),
    USER_META_CHANGE_CODE_SMS_TWILIO_NOT_FOUND("No pending verification found for number",1323),
    USER_META_CHANGE_CODE_SMS_UNEXPECTED_ERROR("Unable to handle SMS provider response",1324),
    MAIL_UNABLE_TO_SEND("Unable to send mail",1400),
    LOCALIZATION_INVALID_RESOURCE_ID("Invalid resource id",1500),
    LOCALIZATION_ALREADY_EXISTS("Localization already exits",1501),
    LOCALIZATION_INVALID_RESOURCE_PARAMETER("Invalid resource parameter (languageCode-key)",1502),
    LOCALIZATION_UNSUPPORTED_REGION("Unsupported region",1503),
    ROLE_INVALID("Invalid role",1600),
    CONTENT_POST_CONTENT_CANNOT_BE_EMPTY("", 1700),
    CONTENT_POST_CAN_REPOST_ONCE("", 1701),
    CONTENT_POST_POSTTYPE_MUST_BE_WATCHLIST("",1702),
    CONTENT_GCP_FILE_STORE_ERROR("An error occurred while storing data to GCS",1703),
    CONTENT_GCP_FILE_COPY_ERROR("An error occurred while copying data to GCS. Temp file not found!",1704),
    CONTENT_GCP_ORIGINAL_FILE_NAME_NULL("Original file name is null",1705),
    CONTENT_GCP_FILE_CONVERSION_ERROR("An error has occurred while converting the file",1706),
    CONTENT_GCP_NOT_PERMITTED_FILE_TYPE("Not a permitted file type",1707),
    CONTENT_GCP_FILE_UPLOAD_ERROR("Error occurred while uploading",1708),
    CONTENT_STORAGE_CANNOT_INITIALIZE("Could not initialize storage",1709),
    CONTENT_STORAGE_FILE_EMPTY("Failed to store empty file",1710),
    CONTENT_STORAGE_WRONG_DIRECTORY("Cannot store file outside current directory",1712),
    CONTENT_STORAGE_CANNOT_STORE_FILE("Cannot store file",1713),
    CONTENT_STORAGE_CANNOT_READ_FILE("Cannot read file",1714),
    CONTENT_STORAGE_CANNOT_READ_STORED_FILE("Cannot read stored file",1715),
    CONTENT_STORAGE_FILE_NOT_FOUND("Cannot find file",1716),
    SOCIAL_USER_FOLLOW_CANNOT_FOLLOW_THEMSELVES("Users cannot follow themselves", 1800),
    SOCIAL_USER_FOLLOW_ALREADY_FOLLOWING("User already following intended user",1801);


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
