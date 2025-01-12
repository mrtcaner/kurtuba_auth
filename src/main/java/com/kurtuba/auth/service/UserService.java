package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.repository.LocalizationAvailableLocaleRepository;
import com.kurtuba.auth.data.repository.UserRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static com.kurtuba.auth.utils.ServiceUtils.validateUserMetaChange;
import static com.kurtuba.auth.utils.Utils.generateVerificationCode;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserTokenService userTokenService;
    private final MessageJobService messageJobService;
    private final UserMetaChangeService userMetaChangeService;
    private final LocalizationAvailableLocaleRepository localizationAvailableLocaleRepository;

    @Value("${kurtuba.meta-change.validity.password-reset-code.minutes}")
    private int passwordResetCodeValidityMinutes;
    @Value("${kurtuba.meta-change.max-try-count}")
    private int metaChangeMaxTryCount;
    @Value("${kurtuba.meta-change.validity.email.activation-code.minutes}")
    private int activationCodeValidityMinutes;
    @Value("${kurtuba.meta-change.validity.email.change-code.minutes}")
    private int emailChangeCodeValidityMinutes;

    public UserService(UserRepository userRepository, UserTokenService userTokenService,
                       MessageJobService messageJobService, UserMetaChangeService userMetaChangeService,
                       LocalizationAvailableLocaleRepository localizationAvailableLocaleRepository) {
        this.userRepository = userRepository;
        this.userTokenService = userTokenService;
        this.messageJobService = messageJobService;
        this.userMetaChangeService = userMetaChangeService;
        this.localizationAvailableLocaleRepository = localizationAvailableLocaleRepository;
    }

    @Transactional
    public void changePassword(@Valid PasswordChangeDto passwordChangeDto, @NotBlank String userId) {
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if (!new BCryptPasswordEncoder().matches(passwordChangeDto.getOldPassword(), user.getPassword())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_WRONG_PASSWORD);
        }

        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getRepeatNewPassword())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH);
        }

        user.setPassword(new BCryptPasswordEncoder().encode(passwordChangeDto.getNewPassword()));
        userRepository.save(user);
        userMetaChangeService.create(UserMetaChange.builder()
                .metaOperationType(MetaOperationType.PASSWORD_CHANGE)
                .userId(user.getId())
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .executed(true)
                .expirationDate(LocalDateTime.now())
                .build());
        messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.PASSWORD_CHANGE, user.getUserSetting().getLocale().getLanguageCode());
    }


    public Optional<User> getUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    public Optional<User> getUserByEmailOrMobile(String email) {
        return userRepository.getUserByEmailOrMobile(email);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.getUserById(id);
    }

    public Optional<User> getUserByMobile(String mobile) {
        return userRepository.getUserByMobile(mobile);
    }

    public Optional<User> getUserByUsername(String userName) {
        return userRepository.getUserByUsername(userName);
    }

    public Optional<User> getUserByUsernameOrEmail(String email) {
        return userRepository.getUserByEmailOrUsername(email);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.getUserByEmail(email).isPresent();
    }

    public boolean isMobileAvailable(String mobile) {
        return !userRepository.getUserByMobile(mobile).isPresent();
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.getUserByUsername(username).isPresent();
    }

    /**
     * Runs when sign in with Google
     *
     * @param username
     */
    @Transactional
    public void processOAuthPostLogin(String username) {
        User existUser = userRepository.getUserByUsername(username).orElse(null);

        if (existUser == null) {
            User newUser = new User();
            newUser.setEmail(username);
            newUser.setAuthProvider(AuthProviderType.GOOGLE);
            newUser.setActivated(true);

            userRepository.save(newUser);

            System.out.println("Created new user: " + username);
        }

    }

    /**
     * User registered and there is already a verified email(email change operation)
     *
     * @param userId
     * @param email
     * @return
     */
    @Transactional
    public UserMetaChange requestChangeEmail(String userId, String email, boolean byCode) {
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (user.isLocked() || user.isShowCaptcha() || !user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if (userRepository.getUserByEmail(email).isPresent()) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaOperationType(MetaOperationType.EMAIL_CHANGE)
                .contactType(ContactType.EMAIL)
                .meta(email)
                .executed(false)
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(emailChangeCodeValidityMinutes))
                .maxTryCount(byCode ? metaChangeMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateVerificationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();
        userMetaChangeService.create(metaChange);

        if (byCode) {
            messageJobService.sendUserEmailChangeCodeMail(email, metaChange.getCode(), user.getUserSetting().getLocale().getLanguageCode());
        } else {
            messageJobService.sendUserEmailChangeLinkMail(email, metaChange.getLinkParam(), user.getUserSetting().getLocale().getLanguageCode());
        }

        return metaChange;

    }

    @Transactional
    public UserMetaChange requestResetPassword(@NotBlank String emailMobile, boolean byCode) {
        User user = userRepository.getUserByEmailOrMobile(emailMobile).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if (emailMobile.contains("@")) {
            //email
            if (!user.isEmailVerified()) {
                throw new BusinessLogicException(ErrorEnum.USER_EMAIL_NOT_VERIFIED);
            }

        } else {
            //mobile
            if (!user.isMobileVerified()) {
                throw new BusinessLogicException(ErrorEnum.USER_MOBILE_NOT_VERIFIED);
            }
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .metaOperationType(MetaOperationType.PASSWORD_RESET)
                .contactType(emailMobile.contains("@") ? ContactType.EMAIL : ContactType.MOBILE)
                .executed(false)
                .expirationDate(LocalDateTime.now().plusMinutes(passwordResetCodeValidityMinutes))
                .maxTryCount(byCode ? metaChangeMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateVerificationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .createdDate(LocalDateTime.now())
                .userId(user.getId())
                .build();

        if (emailMobile.contains("@")) {
            //email
            if (byCode == true) {
                messageJobService.sendPasswordResetCodeMail(user.getEmail(), metaChange.getCode(), user.getUserSetting().getLocale().getLanguageCode());
            } else {
                messageJobService.sendPasswordResetLinkMail(user.getEmail(), metaChange.getLinkParam(), user.getUserSetting().getLocale().getLanguageCode());
            }
        } else {
            //mobile
            // todo implement send sms
            throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
        }
        userMetaChangeService.create(metaChange);

        return metaChange;

    }

    /**
     * If password reset request is responded with a code rather than a link, generated code is random-not unique.
     * Hence, userMetaChangeId is required
     *
     * @param passwordResetByCodeDto
     */
    @Transactional
    public TokensResponseDto resetPasswordByCode(@Valid PasswordResetByCodeDto passwordResetByCodeDto) {

        UserMetaChange userMetaChange = userMetaChangeService
                .findActiveMetaChangeOperationForUser(userRepository
                        .getUserByEmailOrMobile(passwordResetByCodeDto.getEmailMobile()).orElseThrow(() ->
                                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST)
                        ).getId(), MetaOperationType.PASSWORD_RESET).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));

        validatePasswordResetUserMetaChange(userMetaChange, passwordResetByCodeDto.getCode());

        saveNewPassword(userMetaChange, passwordResetByCodeDto.getNewPassword(), passwordResetByCodeDto.getRepeatNewPassword());
        User user = userRepository.getUserById(userMetaChange.getUserId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        return userTokenService.validateRegisteredClientAndGetTokens(user, passwordResetByCodeDto.getClientId(), passwordResetByCodeDto.getClientSecret());

    }

    /**
     * If password reset request is responded with a link, link has unique code.
     *
     * @param passwordResetByLinkDto
     */
    @Transactional
    public void resetPasswordByLink(@Valid PasswordResetByLinkDto passwordResetByLinkDto) {
        UserMetaChange userMetaChange = validatePasswordResetLinkParam(passwordResetByLinkDto.getLinkParam());
        saveNewPassword(userMetaChange, passwordResetByLinkDto.getNewPassword(), passwordResetByLinkDto.getRepeatNewPassword());
    }

    public UserMetaChange validatePasswordResetLinkParam(String linkParam) {
        UserMetaChange userMetaChange = userMetaChangeService.findByLinkParam(linkParam).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));
        validatePasswordResetUserMetaChange(userMetaChange, null);
        return userMetaChange;
    }

    private void validatePasswordResetUserMetaChange(UserMetaChange userMetaChange, String code) {

        if (!userMetaChange.getMetaOperationType().equals(MetaOperationType.PASSWORD_RESET)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION);
        }

        validateUserMetaChange(userMetaChange, code);
    }

    private void saveNewPassword(@NotNull UserMetaChange userMetaChange, String newPassword, String repeatNewPassword) {
        User user = userRepository.getUserById(userMetaChange.getUserId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if (!newPassword.equals(repeatNewPassword)) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH);
        }

        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.create(userMetaChange);

        if (StringUtils.hasLength(user.getEmail())) {
            messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.PASSWORD_RESET, user.getUserSetting().getLocale().getLanguageCode());
        }

        //todo uncomment after SMS integration
        /*if(StringUtils.hasLength(user.getMobile())){
            messageJobService.sendUserMetaChangeNotificationSMS(user.getEmail(), MetaOperationType.PASSWORD_RESET);
        }*/

    }

    @Transactional
    public User saveUser(User user) {
       return userRepository.save(user);
    }

    @Transactional
    public String sendAccountActivationMessage(@NotBlank String emailMobile, boolean byCode) {
        if (emailMobile.contains("@")) {
            //email
            return sendAccountActivationMail(emailMobile, byCode);
        } else {
            return sendAccountActivationSMS(emailMobile, byCode);
        }

    }

    /**
     * Send activation MAIL to the user's email address
     * May contain code or link
     *
     * @return
     */
    @Transactional
    public String sendAccountActivationMail(@NotBlank String email, boolean byCode) {

        User user = userRepository.getUserByEmail(email).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (user.isActivated()) {
            // already activated
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaOperationType(MetaOperationType.ACCOUNT_ACTIVATION)
                .contactType(ContactType.EMAIL)
                .meta(user.getEmail())
                .executed(false)
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(activationCodeValidityMinutes))
                .maxTryCount(byCode ? metaChangeMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateVerificationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();
        userMetaChangeService.create(metaChange);
        if (byCode) {
            messageJobService.sendAccountActivationCodeMail(user.getEmail(), metaChange.getCode(), user.getUserSetting().getLocale().getLanguageCode());
        } else {
            messageJobService.sendAccountActivationLinkMail(user.getEmail(), metaChange.getLinkParam(), user.getUserSetting().getLocale().getLanguageCode());
        }

        return metaChange.getId();


    }

    /**
     * Send activation SMS to the user's mobile number
     * May contain code or link
     *
     * @return
     */
    @Transactional
    public String sendAccountActivationSMS(@NotBlank String mobile, boolean byCode) {
        throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
    }

    @Transactional
    public void updateAccountActivationTryCount(AccountActivationDto accountActivationDto) {
        UserMetaChange userMetaChange = userMetaChangeService
                .findActiveMetaChangeOperationForUser(userRepository
                        .getUserByEmailOrMobile(accountActivationDto.getEmailMobile()).orElseThrow(() ->
                                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST)
                        ).getId(), MetaOperationType.ACCOUNT_ACTIVATION).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));
        updateUserMetaChangeTryCount(userMetaChange);
    }

    @Transactional
    public void updateEmailChangeTryCount(@Valid EmailVerificationDto emailVerificationDto) {
        UserMetaChange userMetaChange = userMetaChangeService
                .findActiveMetaChangeOperationForUser(userRepository
                        .getUserByEmailOrMobile(emailVerificationDto.getEmailMobile()).orElseThrow(() ->
                                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST)
                        ).getId(), MetaOperationType.EMAIL_CHANGE).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));
        updateUserMetaChangeTryCount(userMetaChange);
    }

    @Transactional
    public void updateUserMetaChangeTryCount(UserMetaChange userMetaChange) {
        userMetaChange.setTryCount(userMetaChange.getTryCount() + 1);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.update(userMetaChange);
    }

    @Transactional
    public void updatePasswordResetTryCount(PasswordResetByCodeDto passwordResetByCodeDto) {
        UserMetaChange userMetaChange = userMetaChangeService
                .findActiveMetaChangeOperationForUser(userRepository
                        .getUserByEmailOrMobile(passwordResetByCodeDto.getEmailMobile()).orElseThrow(() ->
                                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST)
                        ).getId(), MetaOperationType.PASSWORD_RESET).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));
        updateUserMetaChangeTryCount(userMetaChange);
    }

    /**
     * Verify email by rest request. User must enter the code mailed to them
     *
     * @param emailMobile
     * @param code
     * @return
     */
    @Transactional
    public UserDto verifyEmailByCode(@NotBlank String emailMobile, @NotBlank String code) {
        User user = userRepository.getUserByEmailOrMobile(emailMobile).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        UserMetaChange userMetaChange = userMetaChangeService
                .findActiveMetaChangeOperationForUser(user.getId(), MetaOperationType.EMAIL_CHANGE).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));

        validateEmailChangeUserMetaChange(userMetaChange, code);

        return saveNewEmail(userMetaChange, user);
    }

    @Transactional
    public UserMetaChange verifyEmailByLink(@NotBlank String linkParam) {
        UserMetaChange userMetaChange = userMetaChangeService.findByLinkParam(linkParam).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));
        validateEmailChangeUserMetaChange(userMetaChange, null);
        User user = userRepository.getUserById(userMetaChange.getUserId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));
        saveNewEmail(userMetaChange, user);
        return userMetaChange;
    }

    private void validateEmailChangeUserMetaChange(UserMetaChange userMetaChange, String code) {

        if (!userMetaChange.getMetaOperationType().equals(MetaOperationType.EMAIL_CHANGE)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION);
        }

        validateUserMetaChange(userMetaChange, code);
    }

    private UserDto saveNewEmail(UserMetaChange userMetaChange, User user) {

        if (StringUtils.hasLength(user.getEmail())) {
            //send change notification mail to old e-mail
            messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.EMAIL_CHANGE, user.getUserSetting().getLocale().getLanguageCode());
        }

        user.setEmail(userMetaChange.getMeta());
        user.setEmailVerified(true);
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.update(userMetaChange);

        return UserDto.fromUser(user);
    }
}
